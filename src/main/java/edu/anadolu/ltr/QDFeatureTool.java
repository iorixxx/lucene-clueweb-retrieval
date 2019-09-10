package edu.anadolu.ltr;

import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tool that computes SEO-based document features.
 */
public class QDFeatureTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Argument
    private List<String> files = new ArrayList<>();

    @Option(name = "-out", required = true, usage = "output file")
    private String out;

    @Override
    public String getShortDescription() {
        return "SEO Tool for ClueWeb09 ClueWeb12 Gov2 collections";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }

    @Override
    public void run(Properties props) throws Exception {

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;


        final String docsPath = props.getProperty("paths.docs." + collection.toString());


        if (docsPath == null) {
            System.out.println(getHelp());
            return;
        }

        String[] spamWiki = new String[]
                {
                        "clueweb09-enwp01-95-02016",
                        "clueweb09-enwp01-90-17134",
                        "clueweb09-enwp02-04-15021",
                        "clueweb09-enwp01-35-03270",
                        "clueweb09-enwp01-15-24594",
                        "clueweb09-enwp03-36-01635",
                        "clueweb09-enwp00-54-16573",
                        "clueweb09-enwp01-76-17822",
                        "clueweb09-enwp01-81-20329",
                        "clueweb09-enwp03-37-21416",
                        "clueweb09-enwp03-15-15563",
                        "clueweb09-enwp01-92-08869",
                        "clueweb09-enwp01-86-03020",
                        "clueweb09-enwp01-84-21637",
                        "clueweb09-enwp01-92-17846"
                };

        Set<String> docIdSet = new HashSet<>();
        List<AbstractMap.SimpleEntry<String, String>> qdPair = new ArrayList<>();
//        docIdSet.addAll(Arrays.asList(spamWiki));

        for (String file : files) {
            System.out.println(file);
            Path path = Paths.get(file);
            if (!(Files.exists(path) && Files.isRegularFile(path))) {
                System.out.println(getHelp());
                return;
            }
            qdPair.addAll(Collection.GOV2.equals(collection) ? retrieveQDPairsForLetor(path) : retrieveQDPairs(path));

            for (AbstractMap.SimpleEntry<String, String> entry : qdPair) {
                docIdSet.add(entry.getValue());
            }
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();

        List<IQDFeature> qdFeatures = new ArrayList<>();
        //qdFeatures.add();

        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();
        TraverserForQD traverserQD = new TraverserForQD(dataset, docsPath, qdPair, qdFeatures);

        traverserQD.traverseParallel(Paths.get(out), numThreads);
        System.out.println("Query Document features are extracted in " + execution(start));

    }

    private List<AbstractMap.SimpleEntry<String, String>> retrieveQDPairs (Path file) throws IOException {

        List<AbstractMap.SimpleEntry<String, String>> qdPairList = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;

            int i = line.indexOf("#");

            if (i == -1) {
                throw new RuntimeException("cannot find # in " + line);
            }

            String qid = "";
            String[] tokens = line.split("\\s+");
            for (String token : tokens) {
                if (!token.startsWith("qid")) continue;
                qid = token.split(":")[1].trim();
            }

            String docId = line.substring(i + 1).trim();

            qdPairList.add(new AbstractMap.SimpleEntry<String, String>(qid, docId));
        }

        lines.clear();

        return qdPairList;
    }


    private List<AbstractMap.SimpleEntry<String, String>> retrieveQDPairsForLetor (Path file) throws IOException {

        List<AbstractMap.SimpleEntry<String, String>> qdPairList = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;

            int i = line.indexOf("GX");

            if (i == -1) {
                throw new RuntimeException("cannot find # in " + line);
            }

            String qid = "";
            String[] tokens = line.split("\\s+");
            for (String token : tokens) {
                if (!token.startsWith("qid")) continue;
                qid = token.split(":")[1].trim();
            }

            String docId = line.substring(i, line.indexOf(" ", i)).trim();

            qdPairList.add(new AbstractMap.SimpleEntry<String, String>(qid, docId));
        }

        lines.clear();

        return qdPairList;
    }


}
