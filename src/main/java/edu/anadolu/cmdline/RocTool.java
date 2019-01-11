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
            for (int i = 0; i < 4; i++)
                solrURLs[i] = getCW09Solr(Ranking.values()[i]);
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

    private void distribution(Ranking ranking) throws IOException {

        int[] relevant = new int[100];
        Arrays.fill(relevant, 0);

        int[] spam = new int[100];
        Arrays.fill(spam, 0);

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
        }

        System.out.println(ranking + ",spam,relevant");

        for (int i = 0; i < 100; i++)
            System.out.println(i + "," + spam[i] + "," + relevant[i]);

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

        distribution(fusion);

    }
}
