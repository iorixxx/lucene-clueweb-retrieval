package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.DataSet;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.ClueWeb09WarcRecord;
import org.clueweb09.ClueWeb12WarcRecord;
import org.clueweb09.Gov2Record;
import org.clueweb09.WarcRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static edu.anadolu.Indexer.BUFFER_SIZE;

/**
 * Traverses for ClueWeb{09|12} plus GOV2
 */
public class Traverser {

    private final class WorkerThread {

        final private Path inputWarcFile;

        WorkerThread(Path inputWarcFile) {
            this.inputWarcFile = inputWarcFile;
        }

        private int processWarcRecord(WarcRecord warcRecord) {
            // see if it's a response record
            if (!Indexer.RESPONSE.equals(warcRecord.type()))
                return 0;

            String id = warcRecord.id();

            if (skip(id)) return 0;

            for (IDocFeature iDoc : featureList) {
                double value = iDoc.calculate(warcRecord);
                System.out.println(id + "\t" + iDoc.toString() + ":" + value);
            }
            return 1;
        }

        private int processClueWeb12WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb12WarcRecord wDoc;
                while ((wDoc = ClueWeb12WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += processWarcRecord(wDoc);
                }
            }
            return i;
        }

        private int processClueWeb09WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb09WarcRecord wDoc;
                while ((wDoc = ClueWeb09WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += processWarcRecord(wDoc);
                }
            }
            return i;
        }

        private int indexGov2File() throws IOException {

            int i = 0;

            StringBuilder builder = new StringBuilder();

            boolean found = false;

            try (
                    InputStream stream = new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {


                for (; ; ) {
                    String line = reader.readLine();
                    if (line == null)
                        break;

                    line = line.trim();

                    if (line.startsWith(Gov2Record.DOC)) {
                        found = true;
                        continue;
                    }

                    if (line.startsWith(Gov2Record.TERMINATING_DOC)) {
                        found = false;
                        WarcRecord gov2 = Gov2Record.parseGov2Record(builder);
                        i += processWarcRecord(gov2);
                        builder.setLength(0);
                    }

                    if (found)
                        builder.append(line).append(" ");
                }
            }

            return i;
        }


        public void run() {
            try {

                Thread.currentThread().setName(inputWarcFile.toAbsolutePath().toString());

                if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection)) {
                    int addCount = processClueWeb09WarcFile();
                    //System.out.println("*./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "  " + addCount);
                } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection)) {
                    int addCount = processClueWeb12WarcFile();
                    //System.out.println("./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "\t" + addCount);
                } else if (Collection.GOV2.equals(collection)) {
                    int addCount = indexGov2File();
                    //System.out.println("./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "\t" + addCount);
                }

            } catch (IOException ioe) {
                System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                ioe.printStackTrace(System.out);
            }
        }
    }

    /**
     * Skip certain documents that hang Jsoup.parse method
     *
     * @param docId document identifier
     * @return true if the document should be skipped
     */
    protected boolean skip(String docId) {
        return "clueweb12-1100wb-15-21381".equals(docId) || "clueweb12-1013wb-14-21356".equals(docId) || !docIdSet.contains(docId);
    }

    private final Path docsPath;
    private final Collection collection;
    private final SolrClient solr;
    private final Set<String> docIdSet;
    private final List<IDocFeature> featureList;

    Traverser(DataSet dataset, String docsDir, HttpSolrClient solr, Set<String> docIdSet, List<IDocFeature> featureList) {
        this.collection = dataset.collection();
        this.docIdSet = docIdSet;
        this.featureList = featureList;

        docsPath = Paths.get(docsDir);
        if (!Files.exists(docsPath) || !Files.isReadable(docsPath) || !Files.isDirectory(docsPath)) {
            System.out.println("Document directory '" + docsPath.toString() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        this.solr = solr;
    }


    /**
     * Traverse based on Java8's parallel streams
     */
    void traverseParallel() throws IOException {

        final String suffix = Collection.GOV2.equals(collection) ? ".gz" : ".warc.gz";

        try (Stream<Path> stream = Files.find(docsPath, 3, new WarcMatcher(suffix))) {

            stream.parallel().forEach(p -> {
                new WorkerThread(p).run();
            });

        }
    }

    private final class WarcMatcher implements BiPredicate<Path, BasicFileAttributes> {

        private final String suffix;

        WarcMatcher(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean test(Path path, BasicFileAttributes basicFileAttributes) {

            if (path.toString().contains("OtherData")) return false;

            if (!basicFileAttributes.isRegularFile()) return false;

            Path name = path.getFileName();

            return (name != null && name.toString().endsWith(suffix));

        }
    }
}
