package edu.anadolu.cmdline;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static edu.anadolu.Indexer.FIELD_CONTENTS;


/**
 * Evaluator tool
 */
public class EvaluatorTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    private void setCollection(String s) {
        String[] parts = s.split("_");
        collections = new Collection[parts.length];
        for (int i = 0; i < parts.length; i++) {
            collections[i] = Collection.valueOf(parts[i]);
        }
        if (parts.length == 1)
            collection = collections[0];
    }

    protected Collection collection;
    protected Collection[] collections;

    @Override
    public String getShortDescription() {
        return "Evaluator utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;


    @Option(name = "-e", metaVar = "[1.0|2.0|5.0]", required = false, usage = "expected under average document length")
    protected double e = 1.0;

    @Option(name = "-rel", metaVar = "[0|27|69]", required = false, usage = "Minimum number of relevant documents")
    protected int minRel = 0;

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();

    @Option(name = "-query", metaVar = "[0|1|2|3|4|5|8]", required = false, usage = "Query set option")
    protected int query = 0;

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "all";

    protected List<InfoNeed> needs;

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-field", metaVar = "[title|body|description|keywords]", required = false, usage = "field that you want to search on")
    protected String field = FIELD_CONTENTS;

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-spamCustomDir", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected String spamDir = null;


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";
        if(spamDir!=null) evalDirectory=spamDir;

        final Evaluator evaluator;
        if (collections.length > 1) {
            DataSet[] dataSets = new DataSet[collections.length];
            String[] evalDirs = new String[collections.length];
            for (int i = 0; i < collections.length; i++) {
                dataSets[i] = CollectionFactory.dataset(collections[i], tfd_home);
                evalDirs[i] = evalDirectory;
            }
            evaluator = new Evaluator(dataSets, tag, measure, models, evalDirs, op);
        } else if (collections.length == 1) {
            DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
            evaluator = new Evaluator(dataSet, tag, measure, models, evalDirectory, op, field);
        } else throw new RuntimeException("Invalid collections " + Arrays.toString(collections));


        evaluator.printTopicModelSortedByVariance();

        Map<String, List<InfoNeed>> bestModelMap = evaluator.absoluteBestModelMap();
        Map<String, Double> riskMap = evaluator.riskSOTA();
        Map<String, Double> ctiMap = evaluator.cti();
        Map<String, Double> zRiskMap = evaluator.zRisk();
        Map<String, Double> geoRiskMap = evaluator.geoRisk();

        Evaluator.printCountMap(bestModelMap, riskMap, ctiMap, zRiskMap, geoRiskMap);


        System.out.println("========= mean effective measures ===========");
        evaluator.printMeanWT();
        evaluator.printMean();

        System.out.println("=========  Random and Oracle ===========");
        System.out.println("Random : " + evaluator.random());
        System.out.println("RandomX : " + evaluator.randomX());
        System.out.println("OracleMin : " + evaluator.oracleMin());
        System.out.println("OracleMax : " + evaluator.oracleMax());

        System.out.println("========= Facets ===========");
        evaluator.printFacets();

    }
}
