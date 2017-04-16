package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.spam.SubmissionFile;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Tool for integration Waterloo Spam Rankings
 */
public final class SpamTool extends CmdLineTool {

    @Option(name = "-tag", metaVar = "[KStemAnalyzer|KStemAnalyzerAnchor]", required = false, usage = "Index Tag")
    protected String tag = "KStemAnalyzer";

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Tool for integration Waterloo Spam Rankings";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.spam paths.docs files.ids files.spam";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if ("index".equals(task)) {

            long start = System.nanoTime();
            int numIndexed = indexWaterlooSpamCW09(props.getProperty("waterloo.spam.CW09"), Paths.get(props.getProperty("waterloo.spam.CW09.lucene")));
            System.out.println("Total " + numIndexed + " lines indexed in " + execution(start));
            start = System.nanoTime();
            numIndexed = indexWaterlooSpamCW12(props.getProperty("waterloo.spam.CW12"), Paths.get(props.getProperty("waterloo.spam.CW12.lucene")));
            System.out.println("Total " + numIndexed + " lines indexed in " + execution(start));
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        if (dataset == null) {
            System.out.println(collection + " returned null dataset");
            return;
        }


        final HttpSolrClient solr;
        if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE1.equals(collection)) {
            solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/spam09A").build();
        } else if (Collection.CW12B.equals(collection))
            solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/spam12A").build();
        else {
            System.out.println("spam filtering is only applicable to ClueWeb09 and ClueWeb12 collections!");
            return;
        }


        List<Path> pathList = Evaluator.discoverTextFiles(dataset.collectionPath().resolve("runs"), ".txt");

        System.out.println("there are " + pathList.size() + " many TREC submission files found to be processed...");

        long start = System.nanoTime();

        for (Path submission : pathList) {

            System.out.println(submission);

            final SubmissionFile submissionFile = new SubmissionFile(submission);

            final Map<Integer, List<SubmissionFile.Tuple>> submissionFileMap = submissionFile.entryMap();

            Path relPath = dataset.collectionPath().resolve("runs").relativize(submission);

            final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(17);

            for (int spamThreshold = 10; spamThreshold <= 90; spamThreshold += 5) {

                Path parallel = dataset.collectionPath().resolve("spam_" + spamThreshold + "_runs").resolve(relPath);

                Path parent = parallel.getParent();

                if (!Files.exists(parent))
                    Files.createDirectories(parent);

                final int s = spamThreshold;

                executor.execute(() -> {
                    try {
                        filterTRECSubmissionFile(submissionFileMap, submissionFile.runTag(), parallel, s, solr);
                    } catch (SolrServerException | IOException ioe) {
                        System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                        ioe.printStackTrace();
                    }
                });

            }

            System.out.println("basliyoruz!");

            //add some delay to let some threads spawn by scheduler
            Thread.sleep(30000);
            executor.shutdown(); // Disable new tasks from being submitted

            try {
                // Wait for existing tasks to terminate
                while (!executor.awaitTermination(3, TimeUnit.MINUTES)) {
                    Thread.sleep(1000);
                    System.out.println(String.format("%.2f percentage completed in ", (executor.getCompletedTaskCount() / 17.0d) * 100.0d) + execution(start));
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            submissionFile.clear();
        }


        System.out.println("Percolator completed in " + execution(start));
        solr.close();
    }

    /**
     * Filter documents from a TREC submission file
     *
     * @throws IOException|SolrServerException
     */
    public static int percentile(HttpSolrClient solr, String docID) throws IOException, SolrServerException {


        SolrDocumentList resp = solr.query(new SolrQuery(docID)).getResults();


        if (resp.size() == 0) {
            System.out.println("cannot find docID " + docID + " in " + solr.getBaseURL());
        }

        if (resp.size() != 1) {
            System.out.println("docID " + docID + " returned " + resp.size() + " many hits!");
        }


        int percentile = (int) resp.get(0).getFieldValue("percentile");

        if (percentile >= 0 && percentile < 100)
            return percentile;
        else throw new RuntimeException("percentile invalid " + percentile);
    }


    /**
     * Index files under waterloo-spam-cw12-decoded folder
     *
     * @param clueweb12spam spam directory
     * @throws IOException
     */
    public static int indexWaterlooSpamCW12(String clueweb12spam, Path indexPath) throws IOException {


        Path clueweb12spamFusion = Paths.get(clueweb12spam);

        if (!Files.isDirectory(clueweb12spamFusion) || !Files.exists(clueweb12spamFusion) || !Files.isReadable(clueweb12spamFusion))
            throw new IllegalArgumentException(clueweb12spamFusion + " directory does not exist or is not a directory");

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(new WhitespaceAnalyzer());

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);


        List<Path> files = Files.walk(clueweb12spamFusion)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".spamPct.gz"))
                .collect(Collectors.toList());

        System.out.println(files.size() + " many .spamPct.gz files found for CW12B");

        for (Path file : files) {

            try (
                    InputStream stream = new GZIPInputStream(Files.newInputStream(file, StandardOpenOption.READ), Indexer.BUFFER_SIZE);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII))) {


                for (; ; ) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    writer.addDocument(line2Document(line));
                }
            }
        }
        files.clear();

        int numIndexed = writer.maxDoc();

        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
        }

        dir.close();
        return numIndexed;
    }

    static Document line2Document(String line) {
        // lines with the following format: percentile-score clueweb-docid
        String[] parts = Track.whiteSpaceSplitter.split(line);
        Document document = new Document();
        document.add(new NumericDocValuesField("percentile", Integer.parseInt(parts[0])));
        document.add(new StringField("id", parts[1], Field.Store.NO));
        return document;
    }

    public static int indexWaterlooSpamCW09(String clueweb09spam, Path indexPath) throws IOException {


        Path clueweb09spamFusion = Paths.get(clueweb09spam);

        if (!Files.isRegularFile(clueweb09spamFusion) || !Files.exists(clueweb09spamFusion) || !Files.isReadable(clueweb09spamFusion))
            throw new IllegalArgumentException(clueweb09spamFusion + " does not exist or is not a file");

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(new WhitespaceAnalyzer());

        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);


        try (BufferedReader reader = Files.newBufferedReader(clueweb09spamFusion, StandardCharsets.US_ASCII)) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;
                writer.addDocument(line2Document(line));
            }
        }

        int numIndexed = writer.maxDoc();

        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
        }

        dir.close();
        return numIndexed;
    }

    /**
     * Filter documents from a TREC submission file
     *
     * @throws IOException
     */
    public static void filterTRECSubmissionFile(Map<Integer, List<SubmissionFile.Tuple>> submissionFileMap, String runTag, Path outPath, int threshold, HttpSolrClient solr) throws IOException, SolrServerException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath, StandardCharsets.US_ASCII));

        for (Map.Entry<Integer, List<SubmissionFile.Tuple>> entry : submissionFileMap.entrySet()) {


            Integer qID = entry.getKey();

            int counter = 0;

            List<SubmissionFile.Tuple> list = entry.getValue();

            for (SubmissionFile.Tuple tuple : list) {

                int percentile = percentile(solr, tuple.docID);

                if (percentile < threshold) {
                    //System.out.println("excluding docID " + docID);
                    continue;
                }

                counter++;

                out.print(qID);
                out.print("\tQ0\t");
                out.print(tuple.docID);
                out.print("\t");
                out.print(counter);
                out.print("\t");
                out.print(tuple.score);
                out.print("\t");
                out.print(runTag);
                out.println();

                if (1000 == counter) break;
            }

            if (1000 != counter) {
                //System.out.println("queryID : " + qID + " returned " + counter + " out of " + list.size() + " documents after " + threshold + " spam filtering for " + submissionFile.runTag());
            }

            /**
             * If you are returning zero documents for a query, instead return the single document
             * clueweb09-en0000-00-00000
             * clueweb12-000000-00-00000 clueweb12-0000wb-00-00000
             *
             */

            if (counter == 0) {
                if (list.get(0).docID.startsWith("clueweb12"))
                    out.println(qID + "\tQ0\tclueweb12-000000-00-00000\t1\t0\t" + runTag);
                else if (list.get(0).docID.startsWith("clueweb09"))
                    out.println(qID + "\tQ0\tclueweb09-000000-00-00000\t1\t0\t" + runTag);
                else
                    throw new RuntimeException("docid: " + list.get(0).docID + " should start either clueweb12 or clueweb09!");
            }

        }

        out.flush();
        out.close();

    }
}