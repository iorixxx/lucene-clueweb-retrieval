package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.cmdline.RocTool.Ranking.fusion;
import static edu.anadolu.cmdline.SpamTool.getSpamSolr;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Tool for intrinsic evaluation of Waterloo Spam Rankings
 */
public class RocTool extends CmdLineTool {

    @Option(name = "-collection", usage = "Collection", metaVar = "CW09A or CW12A")
    private edu.anadolu.datasets.Collection collection = Collection.CW09A;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-rank", required = false, usage = "Spam ranking to be processed")
    private Ranking ranking = fusion;


    @Override
    public String getShortDescription() {
        return "Tool for intrinsic evaluation of Waterloo Spam Rankings";
    }

    /**
     * 4 different sets of spam rankings
     */
    enum Ranking {
        fusion,
        britney,
        groupx,
        uk2006
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.spam paths.docs files.ids files.spam";
    }

    private static HttpSolrClient getCW09Solr(Ranking ranking) {

        if (fusion.equals(ranking))
            return getSpamSolr(Collection.CW09A);

        return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/" + ranking.toString()).build();
    }

    private void raw(String tfd_home) throws IOException, SolrServerException {

        final String[] qRels;
        HttpSolrClient[] solrURLs;

        if (Collection.CW09A.equals(collection)) {
            qRels = new String[]{"qrels.web.51-100.txt", "qrels.web.101-150.txt", "qrels.web.151-200.txt"};
            solrURLs = new HttpSolrClient[4];
            solrURLs[0] = getCW09Solr(Ranking.fusion);
            solrURLs[1] = getCW09Solr(Ranking.britney);
            solrURLs[2] = getCW09Solr(Ranking.groupx);
            solrURLs[3] = getCW09Solr(Ranking.uk2006);

        } else if (Collection.CW12A.equals(collection)) {
            qRels = new String[]{"qrels.web.201-250.txt", "qrels.web.251-300.txt", "qrels.web.301-350.txt", "qrels.web.351-400.txt"};
            solrURLs = new HttpSolrClient[1];
            solrURLs[0] = getSpamSolr(Collection.CW12A);
        } else return;


        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(collection.toString() + ".txt"), StandardCharsets.US_ASCII));

        out.print("queryID,docID,relevance,fusion");
        for (int i = 1; i < solrURLs.length; i++)
            out.print("," + Ranking.values()[i]);
        out.println();


        for (String qRel : qRels) {

            Path path = Paths.get(tfd_home, "topics-and-qrels", qRel);

            final List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

            for (String line : lines) {

                String[] parts = whiteSpaceSplitter.split(line);

                assert parts.length == 4 : "qrels file should contain four columns : " + line;

                int queryID = Integer.parseInt(parts[0]);
                String docID = parts[2];
                int grade = Integer.parseInt(parts[3]);

                out.print(queryID + "," + docID + "," + grade);
                for (HttpSolrClient client : solrURLs) {
                    out.print("," + SpamTool.percentile(client, docID));
                }
                out.println();
            }

            lines.clear();
        }

        out.flush();
        out.close();
        for (HttpSolrClient client : solrURLs) {
            client.close();
        }

    }

    class Struct {
        final Ranking ranking;
        final int[] relevant, spam, non;

        Struct(int[] relevant, int[] spam, int[] non, Ranking ranking) {
            this.relevant = relevant;
            this.spam = spam;
            this.non = non;
            this.ranking = ranking;
        }


        /**
         * label those with percentile-score<70 to be spam
         *
         * @param threshold 70
         * @return elements of confusion matrix
         */
        Confusion classify(int threshold) {

            int tp = 0;
            int tn = 0;

            int fp = 0;
            int fn = 0;

            for (int i = 0; i < threshold; i++) {

                tp += spam[i];

                // false positive: when a relevant document is incorrectly classified as spam
                fp += relevant[i];
            }

            for (int i = threshold; i < 100; i++) {

                tn += relevant[i];

                // false negative: when a spam document is incorrectly classified as non-spam
                fn += spam[i];
            }

            return new Confusion(tp, tn, fp, fn);
        }
    }

    class Confusion {

        final int tp, tn, fp, fn;

        Confusion(int tp, int tn, int fp, int fn) {
            this.tp = tp;
            this.tn = tn;
            this.fp = fp;
            this.fn = fn;
        }

        double precision() {
            return (double) tp / (tp + fp);
        }

        double recall() {
            return (double) tp / (tp + fn);
        }

        double fallout() {
            return (double) fp / (tn + fp);
        }

        double f1() {
            return 2.0d * precision() * recall() / (precision() + recall());
        }

        @Override
        public String toString() {
            return "Confusion{" +
                    "tp=" + tp +
                    ", tn=" + tn +
                    ", fp=" + fp +
                    ", fn=" + fn +
                    '}';
        }
    }

    private Struct distribution(Ranking ranking) throws IOException {

        int[] relevant = new int[100];
        Arrays.fill(relevant, 0);

        int[] spam = new int[100];
        Arrays.fill(spam, 0);

        int[] non = new int[100];
        Arrays.fill(non, 0);

        Set<String> set = new HashSet<>();

        final List<String> lines = Files.readAllLines(Paths.get(collection.toString() + ".txt"), StandardCharsets.US_ASCII);


        for (String line : lines) {

            if (line.startsWith("queryID,docID,relevance,fusion"))
                continue;

            String[] parts = line.split(",");

            assert parts.length == 4 || parts.length == 7 : "raw file should contain four columns : " + line;

            int queryID = Integer.parseInt(parts[0]);
            String docID = parts[1];
            int grade = Integer.parseInt(parts[2]);

            String primaryKey = queryID + "_" + docID;
            if (set.contains(primaryKey)) throw new RuntimeException("duplicate primary key " + primaryKey);
            set.add(primaryKey);

            final int percentile;

            switch (ranking) {
                case fusion:
                    percentile = Integer.parseInt(parts[3]);
                    break;
                case britney:
                    percentile = Integer.parseInt(parts[4]);
                    break;
                case groupx:
                    percentile = Integer.parseInt(parts[5]);
                    break;
                case uk2006:
                    percentile = Integer.parseInt(parts[6]);
                    break;
                default:
                    throw new AssertionError(this);
            }

            if (grade == -2)
                spam[percentile]++;

            if (grade > 0)
                relevant[percentile]++;

            if (grade == 0)
                non[percentile]++;
        }

        //    System.out.println(ranking + ",spam,relevant");
        //    for (int i = 0; i < 100; i++)
        //       System.out.println(i + "," + spam[i] + "," + relevant[i]);

        return new Struct(relevant, spam, non, ranking);

    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if ("raw".equals(task)) {
            raw(tfd_home);
            return;
        }

        if (Collection.CW09A.equals(collection)) {
            Struct fusion = distribution(Ranking.fusion);
            Struct britney = distribution(Ranking.britney);
            Struct groupx = distribution(Ranking.groupx);
            Struct uk2006 = distribution(Ranking.uk2006);


            System.out.println("percentile,fusionSpam,fusionRel,britneySpam,britneyRel,groupxSpam,groupxRel,uk2006Spam,uk2006Rel");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," +
                        fusion.spam[i] + "," + fusion.relevant[i] + "," +
                        britney.spam[i] + "," + britney.relevant[i] + "," +
                        groupx.spam[i] + "," + groupx.relevant[i] + "," +
                        uk2006.spam[i] + "," + uk2006.relevant[i]
                );

            for (int t = 0; t < 100; t++) {

                Confusion f = fusion.classify(t);
                Confusion b = britney.classify(t);
                Confusion g = groupx.classify(t);
                Confusion u = uk2006.classify(t);

                System.out.println(t + "," + f.f1() + "," + b.f1() + "," + g.f1() + "," + u.f1());
            }

            // default setting Fusion with 70% threshold
            System.out.println(fusion.classify(70));

        } else if (Collection.CW12A.equals(collection)) {

            this.ranking = fusion;
            Struct fusion = distribution(Ranking.fusion);
            System.out.println("percentile,fusionSpam,fusionRel");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.spam[i] + "," + fusion.relevant[i]);
        }
    }
}
