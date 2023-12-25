package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.spam.OddsBinning;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.anadolu.cmdline.RocTool.Ranking.fusion;
import static edu.anadolu.cmdline.SpamTool.getSpamSolr;
import static org.apache.solr.common.params.CommonParams.HEADER_ECHO_PARAMS;
import static org.apache.solr.common.params.CommonParams.OMIT_HEADER;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Tool for intrinsic evaluation of Waterloo Spam Rankings
 */
public class RocTool extends CmdLineTool {

    @Option(name = "-collection", usage = "Collection", metaVar = "CW09A or CW12A")
    private edu.anadolu.datasets.Collection collection = Collection.CW09A;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-uniq", required = false, usage = "work on unique documents")
    private boolean uniq = true;

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
        uk2006,
        odds
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.spam paths.docs files.ids files.spam";
    }

    HttpSolrClient getCW09Solr(Ranking ranking) {

        if (fusion.equals(ranking))
            return getSpamSolr(Collection.CW09A, solrBaseURL);

        return new HttpSolrClient.Builder().withBaseSolrUrl(solrBaseURL + ranking.toString()).build();
    }

    private void raw(String tfd_home) throws IOException, SolrServerException {

        final String[] qRels;
        final HttpSolrClient[] solrURLs;

        if (Collection.CW09A.equals(collection)) {
            qRels = new String[]{"qrels.web.51-100.txt", "qrels.web.101-150.txt", "qrels.web.151-200.txt",
                    "qrels.session.201-262.txt", "qrels.session.301-348.txt"};
            solrURLs = new HttpSolrClient[5];
            solrURLs[0] = getCW09Solr(Ranking.fusion);
            solrURLs[1] = getCW09Solr(Ranking.britney);
            solrURLs[2] = getCW09Solr(Ranking.groupx);
            solrURLs[3] = getCW09Solr(Ranking.uk2006);
            solrURLs[4] = getCW09Solr(Ranking.odds);

        } else if (Collection.CW12A.equals(collection)) {
            qRels = new String[]{"qrels.web.201-250.txt", "qrels.web.251-300.txt", "qrels.web.301-350.txt", "qrels.web.351-400.txt",
                    "qrels.session.401-469.txt", "qrels.session.501-560.txt"};
            solrURLs = new HttpSolrClient[1];
            solrURLs[0] = getSpamSolr(Collection.CW12A, solrBaseURL);
        } else if (Collection.NTCIR.equals(collection)) {
            CollectionFactory.dataset(collection, tfd_home);
            qRels = new String[]{"qrels.www.1-100.txt", "qrels.www.101-180.txt"};
            solrURLs = new HttpSolrClient[1];
            solrURLs[0] = getSpamSolr(Collection.CW12A, solrBaseURL);
        } else return;


        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(collection.toString() + ".txt"), StandardCharsets.US_ASCII));

        out.print("queryID,docID,relevance,fusion");
        for (int i = 1; i < solrURLs.length; i++)
            out.print("," + Ranking.values()[i]);
        out.println();


        for (String qRel : qRels) {

            Path path = Paths.get(tfd_home, "topics-and-qrels", qRel);

            System.out.println("processing qrel: " + path);

            final List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

            for (String line : lines) {

                String[] parts = whiteSpaceSplitter.split(line);

                if (parts.length != 4) throw new RuntimeException("qrels file should contain four columns : " + line);
                if (Integer.parseInt(parts[1]) != 0) throw new RuntimeException("second column should be zero");

                int queryID = Integer.parseInt(parts[0]);
                String docID = parts[2];
                int grade = Integer.parseInt(parts[3]);

                out.print(queryID + "," + docID + "," + grade);
                if (Collection.CW09A.equals(collection)) {
                    for (int i = 0; i < 4; i++) {
                        HttpSolrClient client = solrURLs[i];
                        out.print("," + SpamTool.percentile(client, docID));
                    }
                    out.print("," + odds(solrURLs[4], docID));
                } else
                    out.print("," + SpamTool.percentile(solrURLs[0], docID));
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

    public static String odds(HttpSolrClient solr, String docID) throws IOException, SolrServerException {

        SolrQuery query = new SolrQuery(docID).setFields("odds");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        SolrDocumentList resp = solr.query(query).getResults();


        if (resp.size() == 0) {
            System.out.println("cannot find docID " + docID + " in " + solr.getBaseURL());
        }

        if (resp.size() != 1) {
            System.out.println("docID " + docID + " returned " + resp.size() + " many hits!");
        }

        String odds = (String) resp.get(0).getFieldValue("odds");

        resp.clear();
        query.clear();

        double d = Double.parseDouble(odds);

        if (d >= -10.42 && d <= 15.96)
            return odds;
        else throw new RuntimeException("odd ratio is invalid " + odds);
    }

    static private class Struct {
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

        /**
         * label those with probability > 2.5 to be spam
         *
         * @param threshold index of the bin
         * @return elements of confusion matrix
         */
        Confusion classifyOdds(int threshold, int size) {

            int tp = 0;
            int tn = 0;

            int fp = 0;
            int fn = 0;

            for (int i = 0; i <= threshold; i++) {

                tn += relevant[i];

                // false negative: when a spam document is incorrectly classified as non-spam
                fn += spam[i];
            }

            for (int i = threshold + 1; i < size; i++) {

                tp += spam[i];

                // false positive: when a relevant document is incorrectly classified as spam
                fp += relevant[i];
            }

            return new Confusion(tp, tn, fp, fn);
        }
    }

    static private class Confusion {

        int sum() {
            return tp + tn + fp + fn;
        }

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

        Set<String> uniqueSpam = new HashSet<>();
        Set<String> uniqueRelevant = new HashSet<>();
        Set<String> uniqueNon = new HashSet<>();

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
                increment(spam, uniqueSpam, docID, percentile);
            //spam[percentile]++;

            if (grade > 0)
                increment(relevant, uniqueRelevant, docID, percentile);
            //relevant[percentile]++;

            if (grade == 0)
                increment(non, uniqueNon, docID, percentile);
            //non[percentile]++;
        }

        //    System.out.println(ranking + ",spam,relevant");
        //    for (int i = 0; i < 100; i++)
        //       System.out.println(i + "," + spam[i] + "," + relevant[i]);

        return new Struct(relevant, spam, non, ranking);

    }

    private void increment(int[] array, Set<String> set, String docId, int percentile) {
        if (uniq) {
            if (!set.contains(docId)) {
                array[percentile]++;
                set.add(docId);
            }
        } else
            array[percentile]++;
    }

    /**
     * First, use ./run.sh Wiki -o cw12_wiki.txt to identify wikipedia pages of the ClueWeb12 dataset
     */
    private void wiki12() throws IOException, SolrServerException {

        HttpSolrClient client = getSpamSolr(Collection.CW12A, solrBaseURL);

        if (client == null) {
            System.out.println("solr client is null!");
            return;
        }

        int[] wiki = new int[100];
        Arrays.fill(wiki, 0);

        final List<String> lines = Files.readAllLines(Paths.get("cw12_wiki.txt"), StandardCharsets.US_ASCII);

        for (String line : lines) {

            String[] parts = line.split("\\s+");

            if (parts.length != 2)
                throw new RuntimeException("lines length not equal to 2 " + line + " " + line.length());

            int percentile = SpamTool.percentile(client, parts[0]);

            if (percentile >= 0 && percentile < 100)
                wiki[percentile]++;
            else throw new RuntimeException("percentile invalid " + percentile);
        }

        client.close();

        System.out.println("percentile,fusionWiki");
        for (int i = 0; i < 100; i++)
            System.out.println(i + "," + wiki[i]);
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

    private String solrBaseURL = "http://irra-micro:8983/solr/";

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        solrBaseURL = props.getProperty("SOLR.URL");

        if (solrBaseURL == null) {
            System.out.println(getHelp());
            return;
        }

        if ("raw".equals(task)) {
            raw(tfd_home);
            return;
        }

        if ("wiki".equals(task)) {

            if (Collection.CW12A.equals(collection))
                wiki12();
            else
                wiki();
            return;
        }

        if ("odds".equals(task) && Collection.CW09A.equals(collection)) {
            odds();
            return;
        }

        if ("all".equals(task)) {
            allOdds();
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

            System.out.println("percentile,fusionNon,britneyNon,groupxNon,uk2006Non");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.non[i] + "," + britney.non[i] + "," + groupx.non[i] + "," + uk2006.non[i]);

        } else if (Collection.CW12A.equals(collection) || Collection.NTCIR.equals(collection)) {

            Struct fusion = distribution(Ranking.fusion);
            System.out.println("percentile,fusionSpam,fusionRel,fusionNonRel");
            for (int i = 0; i < 100; i++)
                System.out.println(i + "," + fusion.spam[i] + "," + fusion.relevant[i] + "," + fusion.non[i]);

            System.out.println("percentile,f1,recall,fallout");
            for (int t = 0; t < 100; t++) {

                Confusion f = fusion.classify(t);

                System.out.println(t + "," + f.f1() + "," + f.recall() + "," + f.fallout());
            }
        }
    }

    private static Character letter(int i) {
        if (i > 0) return 'R';
        if (i == 0) return 'N';
        if (i == -2) return 'S';

        throw new AssertionError("cannot understand relevance grade " + i);
    }

    public static void main(String[] args) throws IOException {

        Path file = Paths.get("/Users/iorixxx/spam-eval/CW09S.txt");

        final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);

        int counter = 0;

        Set<String> spam = new HashSet<>();
        Set<String> relevant = new HashSet<>();
        Set<String> non = new HashSet<>();
        int r = 0;
        int s = 0;
        int n = 0;
        Map<String, List<Integer>> map = new HashMap<>();

        for (String line : lines) {

            if (line.startsWith("queryID,docID,relevance,fusion"))
                continue;

            String[] parts = line.split(",");

            assert parts.length == 4 || parts.length == 7 : "raw file should contain four columns : " + line;


            String docID = parts[1];
            int grade = Integer.parseInt(parts[2]);

            if (grade > 0) {
                relevant.add(docID);
                r++;
            } else if (grade == -2) {
                spam.add(docID);
                s++;
            } else if (grade == 0) {
                non.add(docID);
                n++;
            }

            List<Integer> judges = map.getOrDefault(docID, new ArrayList<>());
            judges.add(grade);
            map.put(docID, judges);

            counter++;
        }

        System.out.println("spam = " + s + " unique_spam = " + spam.size());
        System.out.println("relevant = " + r + " unique_relevant = " + relevant.size());
        System.out.println("non-relevant = " + n + " unique_non_relevant = " + non.size());

        System.out.println("cw09-spam-wiki : " + spam.stream().filter(s1 -> s1.contains("enwp")).count());
        spam.stream().filter(s1 -> s1.contains("enwp")).forEach(System.out::println);

        final Set<String> set12 = new HashSet<>(Files.readAllLines(Paths.get("cw12_wiki.txt")));
        System.out.println("cw12-spam-wiki : " + spam.stream().filter(set12::contains).count());
        spam.stream().filter(set12::contains).forEach(System.out::println);


        RSN rsn = new RSN();
        RSN rs = new RSN();
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
                    .collect(Collectors.toSet());

            if (set.size() > 2)
                System.out.println(entry.getKey());


            if (set.contains('S') && set.size() > 1) {
                System.out.println(set);
                int S = 0, R = 0, N = 0;
                Set<Integer> q = new HashSet<>();
                for (String line : lines) {

                    if (line.startsWith("queryID,docID,relevance,fusion"))
                        continue;

                    String[] parts = line.split(",");

                    assert parts.length == 4 || parts.length == 7 : "raw file should contain four columns : " + line;

                    String docID = parts[1];

                    if (!docID.equals(entry.getKey())) continue;

                    int grade = Integer.parseInt(parts[2]);

                    int qid = Integer.parseInt(parts[0]);
                    q.add(qid);

                    if (grade > 0) R++;
                    else if (grade == 0) N++;
                    else if (grade == -2) S++;
                }

                switch (set.toString()) {
                    case "[S, N]":
                        sn.increment(S, R, N);
                        sn.incrementQ(q);
                        break;

                    case "[R, S, N]":
                        rsn.increment(S, R, N);
                        rsn.incrementQ(q);
                        break;

                    case "[R, S]":
                        rs.increment(S, R, N);
                        rs.incrementQ(q);
                        break;

                    default:
                        throw new RuntimeException("cannot recognize " + set.toString());

                }

            }
        }

        System.out.println("number of docs judged multiple : " + (map.keySet().size() - counter));
        System.out.println(rsn);
        System.out.println(sn);
        System.out.println(rs);

        if (true) return;
        session(11, 200);
        offset(12, 300, "qrels.session.301-348.txt");
        offset(13, 400, "qrels.session.401-469.txt");
        offset(14, 500, "qrels.session.501-560.txt");

    }

    static class RSN {
        int S = 0, R = 0, N = 0;
        Set<Integer> q = new HashSet<>();

        @Override
        public String toString() {
            if (S == 0) return "";

            StringBuilder builder = new StringBuilder();
            builder.append("S(").append(S).append(")");
            if (R > 0)
                builder.append(" R(").append(R).append(")");
            if (N > 0)
                builder.append(" N(").append(N).append(")");

            builder.append(" q=").append(q.size());

            return builder.toString();
        }

        void increment(int S, int R, int N) {
            this.N += N;
            this.R += R;
            this.S += S;
        }

        void incrementQ(Set<Integer> q) {
            this.q.addAll(q);
        }
    }

    private static void session(int year, int offset) throws IOException {

        Path file = Paths.get("/Users/iorixxx/spam-eval/Session" + year + ".txt");

        final List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);


        final SortedSet<Integer> judgeLevels = new TreeSet<>();

        final Map<Integer, Map<String, Integer>> map = new TreeMap<>();

        Set<String> bogusTopics = new HashSet<>();

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
                    if (judge != -2) {
                        System.out.println(queryID + " *** subtopics must be spam! " + judge + " " + docID);
                        bogusTopics.add(queryID + "_" + docID);
                    }
                }

                if (judge == -2) {
                    if (innerMap.get(docID) != -2) {
                        System.out.println(queryID + " +++ subtopics must be spam! " + innerMap.get(docID) + " " + docID);
                        bogusTopics.add(queryID + "_" + docID);
                    }
                }

                if (judge > innerMap.get(docID))
                    innerMap.put(docID, judge);
            } else
                innerMap.put(docID, judge);

            map.put(queryID, innerMap);
        }


        System.out.println("bogus topics " + bogusTopics.size() + " " + bogusTopics);

        int spam = 0;
        int rel = 0;

        bogusTopics.forEach(s -> {

            int i = s.indexOf('_');
            if (-1 == i) throw new RuntimeException("cannot happen!");
            int queryId = Integer.parseInt(s.substring(0, i));
            String docId = s.substring(i + 1);

            Map<String, Integer> innerMap = map.get(queryId);
            if (innerMap.remove(docId) == null) throw new RuntimeException("cannot happen");
            if (innerMap.isEmpty())
                if (map.remove(queryId) == null) throw new RuntimeException("cannot happen");

        });

        System.out.println(judgeLevels + " num_queries " + map.keySet().size());

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
    }

    private void oddsMap() throws IOException {

        TreeMap<String, Integer> relevant = new TreeMap<>();
        TreeMap<String, Integer> spam = new TreeMap<>();
        TreeMap<String, Integer> non = new TreeMap<>();

        Set<String> set = new HashSet<>();

        final List<String> lines = Files.readAllLines(Paths.get(collection.toString() + ".txt"), StandardCharsets.US_ASCII);

        Set<String> uniqueSpam = new HashSet<>();
        Set<String> uniqueRelevant = new HashSet<>();
        Set<String> uniqueNon = new HashSet<>();

        for (String line : lines) {

            if (line.startsWith("queryID,docID,relevance,fusion"))
                continue;

            String[] parts = line.split(",");

            if (parts.length != 8) throw new RuntimeException("raw file should contain 8 columns : " + line);

            int queryID = Integer.parseInt(parts[0]);
            String docID = parts[1];
            int grade = Integer.parseInt(parts[2]);

            String primaryKey = queryID + "_" + docID;
            if (set.contains(primaryKey)) throw new RuntimeException("duplicate primary key " + primaryKey);
            set.add(primaryKey);

            String odds = parts[7];

            double d = Double.parseDouble(odds);

            if (!(d >= -10.42 && d <= 15.96))
                throw new RuntimeException("odd ratio is invalid " + odds);

            if (grade == -2)
                increment(spam, uniqueSpam, docID, odds);


            if (grade > 0)
                increment(relevant, uniqueRelevant, docID, odds);


            if (grade == 0)
                increment(non, uniqueNon, docID, odds);

        }

        System.out.println("odds,relevant");
        relevant.forEach((k, v) -> System.out.println(k + "," + v));

        System.out.println("odds,spam");
        spam.forEach((k, v) -> System.out.println(k + "," + v));

    }

    private void increment(TreeMap<String, Integer> map, Set<String> set, String docId, String odds) {
        if (uniq) {
            if (!set.contains(docId)) {
                final int i = map.getOrDefault(odds, 0) + 1;
                map.put(odds, i);
                set.add(docId);
            }
        } else {
            final int i = map.getOrDefault(odds, 0) + 1;
            map.put(odds, i);
        }
    }

    private void odds() throws IOException {

        final int size = OddsBinning.intervals.length - 1;

        int[] relevant = new int[size];
        Arrays.fill(relevant, 0);

        int[] spam = new int[size];
        Arrays.fill(spam, 0);

        int[] non = new int[size];
        Arrays.fill(non, 0);

        Set<String> set = new HashSet<>();

        final List<String> lines = Files.readAllLines(Paths.get(collection.toString() + ".txt"), StandardCharsets.US_ASCII);

        Set<String> uniqueSpam = new HashSet<>();
        Set<String> uniqueRelevant = new HashSet<>();
        Set<String> uniqueNon = new HashSet<>();

        for (String line : lines) {

            if (line.startsWith("queryID,docID,relevance,fusion"))
                continue;

            String[] parts = line.split(",");

            if (parts.length != 8) throw new RuntimeException("raw file should contain 8 columns : " + line);

            int queryID = Integer.parseInt(parts[0]);
            String docID = parts[1];
            int grade = Integer.parseInt(parts[2]);

            String primaryKey = queryID + "_" + docID;
            if (set.contains(primaryKey)) throw new RuntimeException("duplicate primary key " + primaryKey);
            set.add(primaryKey);

            double d = Double.parseDouble(parts[7]);

            if (!(d >= -10.42 && d <= 15.96))
                throw new RuntimeException("odd ratio is invalid " + d);

            int bin = OddsBinning.bin(d);

            if (grade == -2)
                increment(spam, uniqueSpam, docID, bin);


            if (grade > 0)
                increment(relevant, uniqueRelevant, docID, bin);


            if (grade == 0)
                increment(non, uniqueNon, docID, bin);

        }

        int sum = IntStream.of(spam).sum() + IntStream.of(relevant).sum();

        Struct struct = new Struct(relevant, spam, non, Ranking.odds);

        System.out.println("bin,oddsSpam,oddsRel,oddsNon");
        for (int i = 0; i < size; i++)
            System.out.println(i + "," + struct.spam[i] + "," + struct.relevant[i] + "," + struct.non[i]);

        for (int t = 0; t < size; t++) {
            Confusion f = struct.classifyOdds(t, size);
            if (sum != f.sum())
                throw new RuntimeException("t=" + t + " " + sum + " does not equal " + f.sum() + " " + f.toString());
            System.out.println(t + "," + f.f1() + "," + f.recall() + "," + f.precision());
        }
    }

    /**
     * The frequency distribution of all ClueWeb09's documents over log-odds version of the Fusion ranking.
     */
    private void allOdds() {

        final int size = OddsBinning.intervals.length - 1;

        int[] all = new int[size];
        Arrays.fill(all, 0);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get("/home/iorixxx/clueweb09spam.FusionLogOdds"))) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;

                String[] parts = whiteSpaceSplitter.split(line);

                if (parts.length != 2)
                    throw new RuntimeException("clueweb09spam.FusionLogOdds file should contain 2 columns : " + line);

                double d = Double.parseDouble(parts[0]);

                if (!(d >= -10.42 && d <= 15.96))
                    throw new RuntimeException("odd ratio is invalid " + d);

                int bin = OddsBinning.bin(d);

                all[bin]++;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        System.out.println("bin,all");
        for (int i = 0; i < size; i++)
            System.out.println(i + "," + all[i]);
    }
}