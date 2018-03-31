package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.eval.Evaluator.discoverTextFiles;
import static org.apache.solr.common.params.CommonParams.HEADER_ECHO_PARAMS;
import static org.apache.solr.common.params.CommonParams.OMIT_HEADER;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Tool for Sampling phase of learning to rank.
 * Saves sample list of 8 models decorated with relevance labels.
 * 0 is used for un-judged documents.
 */
public class SampleTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "result list judgements";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;

    @Option(name = "-tag", metaVar = "[KStem KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();


    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-path", metaVar = "/home/iorixxx/features", required = false, usage = "/home/iorixxx/features")
    private String path = null;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        if (path != null) {

            List<List<String>> dupDOCNOlist = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(tfd_home, "topics-and-qrels", "dupDOCNOlist.txt"))) {
                dupDOCNOlist.add(Arrays.asList(whiteSpaceSplitter.split(line)));
            }

            Path path = Paths.get(this.path);
            final Deque<Path> files = Indexer.discoverWarcFiles(path, ".features");

            for (Path in : files) {
                System.out.println("Processing " + in.toString());
                addPageRank(in, path.resolve("X." + in.getFileName().toString()), dupDOCNOlist);
            }
            return;
        }


        List<InfoNeed> needs = dataSet.getTopics();

        Set<String> models = new TreeSet<>();

        for (String parametricModel : parametricModels)
            models.add(ParamTool.train(parametricModel, dataSet, tag, measure, "OR").toString());

        System.out.println("========= best parameters ===========");
        System.out.println(models);

        models.add("DFIC");
        models.add("DPH");
        models.add("DFRee");
        models.add("DLH13");


        String runsDirectory = spam == 0 ? "runs" : "spam_" + spam + "_runs";


        Path samplePath = dataSet.collectionPath().resolve("samples");
        if (!Files.exists(samplePath))
            Files.createDirectory(samplePath);


        for (Track track : dataSet.tracks()) {

            Path thePath = dataSet.collectionPath().resolve(runsDirectory).resolve(tag).resolve(track.toString());

            if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
                throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

            List<Path> paths = discoverTextFiles(thePath, "_OR_all.txt");

            for (String model : models) {

                Path outPath = samplePath.resolve(track.toString() + "." + Evaluator.prettyModel(model) + ".txt");
                PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath, StandardCharsets.US_ASCII));

                int c = 0;
                for (Path path : paths) {
                    if (path.getFileName().toString().startsWith(model + "_")) {
                        c++;
                        List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

                        for (String s : lines) {

                            String[] parts = whiteSpaceSplitter.split(s);

                            if (parts.length != 6)
                                throw new RuntimeException("submission file does not contain 6 columns " + s);

                            final String docId = parts[2];

                            final int qID = Integer.parseInt(parts[0]);

                            InfoNeed need = new InfoNeed(qID, "", track, Collections.emptyMap());

                            int i = needs.indexOf(need);
                            if (-1 == i) {
                                System.out.println("cannot find information need " + qID);
                                continue;
                            }
                            final int judge = needs.get(i).getJudgeMap().getOrDefault(docId, 0);
                            out.println(qID + " 0 " + docId + " " + Integer.toString(judge));

                        }
                    }
                }
                if (c != 1) throw new RuntimeException(c + " many files start with for model " + model);

                out.flush();
                out.close();
            }
        }


        for (String model : models) {

            Path unifiedPath = samplePath.resolve(dataSet.collection().toString() + "." + Evaluator.prettyModel(model) + ".txt");

            final PrintWriter out = new PrintWriter(Files.newBufferedWriter(unifiedPath, StandardCharsets.US_ASCII));

            for (Track track : dataSet.tracks()) {

                Path localPath = samplePath.resolve(track.toString() + "." + Evaluator.prettyModel(model) + ".txt");

                List<String> lines = Files.readAllLines(localPath, StandardCharsets.US_ASCII);

                for (String line : lines)
                    out.println(line);

                out.flush();
                lines.clear();

            }
            out.flush();
            out.close();
        }
    }

    private void addPageRank(Path in, Path out, List<List<String>> dupDOCNOlist) throws IOException, SolrServerException {

        final HttpSolrClient rankSolr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/rank09A").build();
        final HttpSolrClient spamSolr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/spam09A").build();


        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.US_ASCII);
             PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.US_ASCII))) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;

                if (line.startsWith("#"))
                    continue;

                // 0 qid:1 1:81.0 2:86.0 3:122.0 4:122.0 5:50.0 6:86.0 7:86.0 8:3.0 9:3.0 10:81.0 11:81.0 12:18.444643 13:247.42574 14:3.0 15:827.0 16:3.4250813 17:2.9271529 18:7.4784217 19:-2.817856 20:17.85046 # clueweb09-en0010-79-02218
                int i = line.indexOf("#");

                if (i == -1) {
                    System.out.println(line);
                    continue;
                }

                String p1 = line.substring(0, i).trim();
                String docId = line.substring(i + 1).trim();

                int spam = SpamTool.percentile(spamSolr, docId);
                double rank = pageRank(rankSolr, docId, dupDOCNOlist);

                writer.println(p1 + " 21:" + spam + " 22:" + rank + " # " + docId);

            }
        }

        spamSolr.close();
        rankSolr.close();
    }

    /**
     * Retrieve page rank of a given document id e.g., clueweb09-enwp01-38-03709
     * <p>
     * The lists contain 502,511,675 DOCNOs after deduplication. All duplicate URLs are removed from this list, during translating node_ids to WARC DOCNOs.
     * For DOCNOs with the same URL, only the smallest DOCNO is kept as the DOCNO for all the node_ids corresponding to that URL.
     */
    private static double pageRank(HttpSolrClient solr, String docID, List<List<String>> dupDOCNOlist) throws IOException, SolrServerException {

        SolrQuery query = new SolrQuery(docID).setFields("rank");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        SolrDocumentList resp = solr.query(query).getResults();


        /**
         * Look up in the duplicate record list
         */
        if (resp.size() == 0) {

            for (List<String> list : dupDOCNOlist) {
                if (list.contains(docID)) {
                    for (String id : list) {

                        if (docID.equals(id)) continue;

                        double pRank = pageRank(solr, id);
                        if (pRank != -1.0) {
                            System.out.println("Not found in the pagerank data, but found in the duplicate record list: " + docID + " " + pRank);
                            return pRank;
                        }
                    }
                }
            }

            System.out.println("There are a small number of DOCNOs that are not in the pagerank data and are not included in the duplicate record list: " + docID);
            return -1.0;
        }

        if (resp.size() != 1) {
            System.out.println("docID " + docID + " returned " + resp.size() + " many hits!");
        }

        double rank = (double) resp.get(0).getFieldValue("rank");

        resp.clear();
        query.clear();

        return rank;
    }

    private static double pageRank(HttpSolrClient solr, String docID) throws IOException, SolrServerException {

        SolrQuery query = new SolrQuery(docID).setFields("rank");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        SolrDocumentList resp = solr.query(query).getResults();


        if (resp.size() == 0) {
            return -1.0;
        }

        if (resp.size() != 1) {
            System.out.println("docID " + docID + " returned " + resp.size() + " many hits!");
        }

        double rank = (double) resp.get(0).getFieldValue("rank");

        resp.clear();
        query.clear();

        return rank;
    }

    public static void main(String[] args) {

        String line = "0 qid:1 1:81.0 2:86.0 3:122.0 4:122.0 5:50.0 6:86.0 7:86.0 8:3.0 9:3.0 10:81.0 11:81.0 12:18.444643 13:247.42574 14:3.0 15:827.0 16:3.4250813 17:2.9271529 18:7.4784217 19:-2.817856 20:17.85046 # clueweb09-en0010-79-02218";

        int i = line.indexOf("#");

        if (i == -1) {
            System.out.println(line);

        }

        String p1 = line.substring(0, i).trim();
        String docId = line.substring(i + 1).trim();
        System.out.println(p1);
        System.out.println(docId);

        int spam = 50;
        double rank = 455.564546;
        System.out.println(p1 + " 21:" + spam + " 22:" + rank + " # " + docId);

    }
}
