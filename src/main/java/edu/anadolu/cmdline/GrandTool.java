package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Solution;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Grand tool
 */
public class GrandTool extends CmdLineTool {

    protected String tfd_home;
    protected XSSFWorkbook workbook;

    @Option(name = "-optimize", required = false, usage = "Collection")
    protected Measure optimize = Measure.NDCG100;

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    private void setCollection(String s) {
        String[] parts = s.split("_");
        collections = new Collection[parts.length];
        for (int i = 0; i < parts.length; i++) {
            collections[i] = Collection.valueOf(parts[i]);
        }
        Arrays.sort(collections);
    }

    protected Collection[] collections;


    String tag = "KStemAnalyzer";
    String op = "OR";

    public String evalDirectory(DataSet dataset, Measure measure) {
        if (Collection.GOV2.equals(dataset.collection()) || Collection.MC.equals(dataset.collection()) || Collection.ROB04.equals(dataset.collection())) {
            return "evals";
        } else {
            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, op);
            return bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }
    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        workbook = new XSSFWorkbook();

        DataSet[] dataSets = new DataSet[collections.length];
        String[] evalDirs = new String[collections.length];

        for (int i = 0; i < collections.length; i++) {
            dataSets[i] = CollectionFactory.dataset(collections[i], tfd_home);
            evalDirs[i] = evalDirectory(dataSets[i], optimize);
        }

        System.out.println(Arrays.toString(dataSets));
        System.out.println(Arrays.toString(evalDirs));

        Evaluator evaluator = new Evaluator(dataSets, tag, optimize, "all", evalDirs, op);

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

        List<Solution> accuracyList = evaluator.modelsAsSolutionList(evaluator.residualNeeds());

        accuracyList.sort((Solution o1, Solution o2) -> (int) Math.signum(o2.sigma1 - o1.sigma1));
        System.out.println("Number of hits (percentage) : ");
        accuracyList.forEach(System.out::println);

    }


    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getShortDescription() {
        return null;
    }
}
