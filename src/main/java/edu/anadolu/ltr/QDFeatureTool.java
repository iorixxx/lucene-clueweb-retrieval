package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DLH13;
import edu.anadolu.similarities.DPH;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tool that computes QD Features .
 */
public class QDFeatureTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Argument
    private List<String> files = new ArrayList<>();

    @Option(name = "-out", required = true, usage = "output file")
    private String out;

    @Option(name = "-tag", usage = "If you want to search specific tag, e.g. KStemField")
    private String tag = null;

    @Override
    public String getShortDescription() {
        return "QD Feature Tool for ClueWeb09 ClueWeb12 Gov2 collections";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }



    private Tag analyzerTag;
    private IndexReader reader;
    private String indexTag;

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

        Set<String> docIdSet = new HashSet<>();
        List<AbstractMap.SimpleEntry<String, String>> qdPair = new ArrayList<>();

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


        ///////////////////////////// Index Reading for stats ///////////////////////////////////////////
        Path indexPath=null;
        if(this.tag == null)
            indexPath = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory).iterator().next();
        else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory)) {
                for (Path path : stream) {
                    if(!tag.equals(path.getFileName().toString())) continue;
                    indexPath = path;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(indexPath == null)
            throw new RuntimeException(tag + " index not found");


        this.indexTag = indexPath.getFileName().toString();
        this.analyzerTag = Tag.tag(indexTag);

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        IndexSearcher searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(Indexer.FIELD_CONTENTS);

        Map<String, TermStatistics> termStatisticsMap = new HashMap<>();
        for(InfoNeed need : dataset.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query(), Analyzers.analyzer(analyzerTag));

            for (String word : subParts) {
                if (termStatisticsMap.containsKey(word)) continue;
                Term term = new Term(Indexer.FIELD_CONTENTS, word);
                TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
                termStatisticsMap.put(word, termStatistics);
            }

        }

        ///////////////////////////// Index Reading for stats ///////////////////////////////////////////
        long start = System.nanoTime();

        List<IQDFeature> qdFeatures = new ArrayList<>();
        qdFeatures.add(new WMWD_BM25());
        qdFeatures.add(new WMWD_LGD());
        qdFeatures.add(new WMWD_PL2());
        qdFeatures.add(new WMWD_DirichletLM());
        qdFeatures.add(new WMWD_DFIC());
        qdFeatures.add(new WMWD_DPH());
        qdFeatures.add(new WMWD_DLH13());
        qdFeatures.add(new WMWD_DFRee());

        qdFeatures.add(new VariantsOfTfFor5Fields());
        qdFeatures.add(new VariantsOfTfIdfFor5Fields());
        qdFeatures.add(new VariantsOfTermCountLengthNormFor5Fields());
        qdFeatures.add(new CoveredTermCount());
        qdFeatures.add(new CoveredTermRatio());
        qdFeatures.add(new MinCoverageForTitle());
        qdFeatures.add(new MinCoverageForBody());



        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();
        TraverserForQD traverserQD = new TraverserForQD(dataset, docsPath, qdPair, qdFeatures, collectionStatistics, termStatisticsMap, analyzerTag, docIdSet);

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

    static HttpSolrClient solrClientFactory(Collection collection) {

        if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {
            return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor09A").build();
        } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection) || Collection.NTCIR.equals(collection)) {
            return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor12A").build();
        }

        throw new RuntimeException("The factory cannot find appropriate SolrClient for " + collection);
    }
    

}
