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
import java.util.stream.Collectors;

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
            qRels = new String[]{"qrels.web.51-100.txt", "qrels.web.101-150.txt", "qrels.web.151-200.txt",
                    "qrels.session.201-262.txt", "qrels.session.301-348.txt"};
            solrURLs = new HttpSolrClient[4];
            solrURLs[0] = getCW09Solr(Ranking.fusion);
            solrURLs[1] = getCW09Solr(Ranking.britney);
            solrURLs[2] = getCW09Solr(Ranking.groupx);
            solrURLs[3] = getCW09Solr(Ranking.uk2006);

        } else if (Collection.CW12A.equals(collection)) {
            qRels = new String[]{"qrels.web.201-250.txt", "qrels.web.251-300.txt", "qrels.web.301-350.txt", "qrels.web.351-400.txt",
                    "qrels.session.401-469.txt", "qrels.session.501-560.txt"};
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

                if (parts.length != 4) throw new RuntimeException("qrels file should contain four columns : " + line);
                if (Integer.parseInt(parts[1]) != 0) throw new RuntimeException("second column should be zero");

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

    private void wiki() throws IOException {

        int[] fusion = wiki("wFusion.txt");
        int[] britney = wiki("wBritney.txt");
        int[] groupx = wiki("wGroupX.txt");
        int[] uk2006 = wiki("wUK.txt");

        System.out.println("percentile,fusionWiki,britneyWiki,groupxWiki,uk2006Wiki");
        for (int i = 0; i < 100; i++)
            System.out.println(i + "," + fusion[i] + "," + britney[i] + "," + groupx[i] + "," + uk2006[i]);

    }

    // Use grep tool to identify wikipedia pages whose docIds start with enwp*
    // grep enwp clueweb09spam.GroupX > wGroupX.txt

    private int[] wiki(String wikiPages) throws IOException {

        int[] wiki = new int[100];
        Arrays.fill(wiki, 0);

        final List<String> lines = Files.readAllLines(Paths.get(wikiPages), StandardCharsets.US_ASCII);

        for (String line : lines) {

            String[] parts = line.split("\\s+");

            if (parts.length != 2) throw new RuntimeException("lines length not equal to 2");

            int percentile = Integer.parseInt(parts[0]);

            if (percentile >= 0 && percentile < 100)
                wiki[percentile]++;
            else throw new RuntimeException("percentile invalid " + percentile);
        }
        return wiki;
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

        if ("wiki".equals(task)) {
            wiki();
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

            // print recall and fallout necessary for ROC curves
            for (int t = 0; t < 100; t++) {

                Confusion f = fusion.classify(t);
                Confusion b = britney.classify(t);
                Confusion g = groupx.classify(t);
                Confusion u = uk2006.classify(t);
                System.out.println(t + "," +
                        f.recall() + "," + f.fallout() + "," +
                        b.recall() + "," + b.fallout() + "," +
                        g.recall() + "," + g.fallout() + "," +
                        u.recall() + "," + u.fallout());
            }

            //

            System.out.println("percentile,fusionNon,britneyNon,groupxNon,uk2006Non");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.non[i] + "," + britney.non[i] + "," + groupx.non[i] + "," + uk2006.non[i]);

        } else if (Collection.CW12A.equals(collection)) {

            Struct fusion = distribution(Ranking.fusion);
            System.out.println("percentile,fusionSpam,fusionRel");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.spam[i] + "," + fusion.relevant[i]);


            for (int t = 0; t < 100; t++) {

                Confusion f = fusion.classify(t);

                System.out.println(t + "," + f.f1() + "," + f.recall() + "," + f.fallout());
            }

            System.out.println("percentile,fusionNon");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.non[i]);
        }
    }

    private static Character letter(int i) {
        if (i > 0) return 'R';
        if (i == 0) return 'N';
        if (i == -2) return 'S';

        throw new AssertionError("cannot understand relevance grade " + i);
    }

    public static void main(String[] args) throws IOException {

        Path file = Paths.get("/Users/iorixxx/spam-eval/CW09A.txt");

        final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);

        int counter = 0;

        Map<String, List<Integer>> map = new HashMap<>();

        for (String line : lines) {

            if (line.startsWith("queryID,docID,relevance,fusion"))
                continue;

            String[] parts = line.split(",");

            assert parts.length == 4 || parts.length == 7 : "raw file should contain four columns : " + line;


            String docID = parts[1];
            int grade = Integer.parseInt(parts[2]);


            List<Integer> judges = map.getOrDefault(docID, new ArrayList<>());
            judges.add(grade);
            map.put(docID, judges);

            counter++;
        }

        RSN rsn = new RSN();
        RSN sr = new RSN();
        RSN sn = new RSN();

        System.out.println("document-query pairs : " + counter + " distinct documents : " + map.keySet().size());

        counter = 0;
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {

            if (entry.getValue().size() == 1) {
                counter++;
                continue;
            }

            Set<Character> set = entry.getValue()
                    .stream()
                    .map(RocTool::letter)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toSet());

            if (set.size() > 2)
                System.out.println(entry.getKey());


            if (set.contains('S') && set.size() > 1) {
                System.out.println(set);
                int S = 0, R = 0, N = 0;
                for (String line : lines) {

                    if (line.startsWith("queryID,docID,relevance,fusion"))
                        continue;

                    String[] parts = line.split(",");

                    assert parts.length == 4 || parts.length == 7 : "raw file should contain four columns : " + line;

                    String docID = parts[1];

                    if (!docID.equals(entry.getKey())) continue;

                    int grade = Integer.parseInt(parts[2]);

                    if (grade > 0) R++;
                    else if (grade == 0) N++;
                    else if (grade == -2) S++;
                }

                switch (set.toString()) {
                    case "[S, N]":
                        sn.increment(S, R, N);
                        break;

                    case "[R, S, N]":
                        rsn.increment(S, R, N);
                        break;

                    case "[R, S]":
                        sr.increment(S, R, N);
                        break;

                    default:
                        throw new RuntimeException("cannot recognize " + set.toString());

                }

            }
        }

        System.out.println("number of docs judged multiple : " + (map.keySet().size() - counter));
        System.out.println(rsn);
        System.out.println(sr);
        System.out.println(sn);

        session(11, 200);
        offset(12, 300, "qrels.session.301-348.txt");
        offset(13, 400, "qrels.session.401-469.txt");
        offset(14, 500, "qrels.session.501-560.txt");

    }

    static class RSN {
        int S = 0, R = 0, N = 0;

        @Override
        public String toString() {
            if (S == 0) return "";

            StringBuilder builder = new StringBuilder();
            builder.append("S(").append(S).append(")");
            if (R > 0)
                builder.append(" R(").append(R).append(")");
            if (N > 0)
                builder.append(" N(").append(N).append(")");

            return builder.toString();
        }

        void increment(int S, int R, int N) {
            this.N += N;
            this.R += R;
            this.S += S;
        }
    }

    private static void session(int year, int offset) throws IOException {

        Path file = Paths.get("/Users/iorixxx/spam-eval/Session" + year + ".txt");

        final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);


        final SortedSet<Integer> judgeLevels = new TreeSet<>();

        final Map<Integer, Map<String, Integer>> map = new TreeMap<>();

        for (String line : lines) {


            line = line.trim();

            String[] parts = whiteSpaceSplitter.split(line);

            if (parts.length != 4) throw new RuntimeException("qrels file should contain four columns : " + line);


            int queryID = Integer.parseInt(parts[0]) + offset;
            String docID = parts[2];
            int judge = Integer.parseInt(parts[3]);


            judgeLevels.add(judge);

            Map<String, Integer> innerMap = map.getOrDefault(queryID, new HashMap<>());

            /*
            For the task completion, given a query, NIST assessors assigned document multiple relevance grades, each for possible tasks provided in ground truth.
            For Adhoc, we derived document relevance by using the maximum relevance label assigned for that document over all possible tasks.
            */
            if (innerMap.containsKey(docID)) {

                if (innerMap.get(docID) == -2) {
                    if (judge != -2) System.out.println("*** subtopics must be spam! " + judge + " " + docID);
                }

                if (judge == -2) {
                    if (innerMap.get(docID) != -2)
                        System.out.println("+++ subtopics must be spam! " + innerMap.get(docID) + " " + docID);
                    //  innerMap.put(docID, judge);
                }

                //   if (innerMap.get(docID) != -2)
                if (judge > innerMap.get(docID))
                    innerMap.put(docID, judge);
            } else
                innerMap.put(docID, judge);

            map.put(queryID, innerMap);
        }

        System.out.println(judgeLevels + " num_queries " + map.keySet().size());

        int spam = 0;
        int rel = 0;

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(Paths.get("/Users/iorixxx/spam-eval/qrels.session.201-262.txt"), StandardCharsets.US_ASCII));

        for (Map.Entry<Integer, Map<String, Integer>> entry : map.entrySet()) {

            int queryID = entry.getKey();

            Map<String, Integer> innerMap = entry.getValue();

            for (Map.Entry<String, Integer> e : innerMap.entrySet()) {
                String docID = e.getKey();
                int judge = e.getValue();

                if (judge < -2 || judge > 3)
                    throw new IllegalArgumentException("unexpected judge level for session track [-2, 0, 1, 2, 3] " + judge);

                if (judge == -2) spam++;
                if (judge > 0) rel++;

                output.println(queryID + " 0 " + docID + " " + judge);
            }
        }

        output.flush();
        output.close();

        System.out.println("rel=" + rel + " spam=" + spam);


    }

    private static void offset(int year, int offset, String out) throws IOException {

        Path file = Paths.get("/Users/iorixxx/spam-eval/Session" + year + ".txt");

        final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);


        final SortedSet<Integer> judgeLevels = new TreeSet<>();

        Set<Integer> queries = new HashSet<>();

        int spam = 0;
        int rel = 0;

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(Paths.get("/Users/iorixxx/spam-eval/" + out), StandardCharsets.US_ASCII));


        for (String line : lines) {


            line = line.trim();

            String[] parts = whiteSpaceSplitter.split(line);

            if (parts.length != 4) throw new RuntimeException("qrels file should contain four columns : " + line);


            int queryID = Integer.parseInt(parts[0]) + offset;

            queries.add(queryID);
            String docID = parts[2];
            int judge = Integer.parseInt(parts[3]);

            if (Integer.parseInt(parts[1]) != 0) throw new RuntimeException("second column should be zero");

            if (judge == -2) spam++;
            if (judge > 0) rel++;

            judgeLevels.add(judge);


            output.println(queryID + " 0 " + docID + " " + judge);

        }
        output.flush();
        output.close();
        System.out.println(judgeLevels + " num_queries " + queries.size());
        System.out.println("rel=" + rel + " spam=" + spam);

        System.out.println("rel=" + String.format("%.1f", (double) rel / queries.size()) + " spam=" + String.format("%.1f", (double) spam / queries.size()));
        ;
    }
}
