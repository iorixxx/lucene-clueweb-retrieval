package edu.anadolu.mc;

import edu.anadolu.datasets.Collection;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import org.clueweb09.InfoNeed;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Evaluator for Milliyet Collection
 */
public class MCEvaluator {


    public static void main(String[] args) throws Exception {
        String tfd_home = "/Users/iorixxx/TFD_HOME";

        String tag = "NS";


        MCSet mcSet = new MCSet(tfd_home);

        Path collectionPath = Paths.get(tfd_home, Collection.MC.toString());


        Evaluator evaluator = new Evaluator(mcSet, tag, Measure.NDCG100, "all", "evals", "OR");
        evaluator.printSortedTopicModel();

        Map<String, List<InfoNeed>> bestModelMap = evaluator.bestModelMap();
        Map<String, Double> riskMap = evaluator.riskSOTA();
        Map<String, Double> ctiMap = evaluator.cti();
        Map<String, Double> zRiskMap = evaluator.zRisk();
        Map<String, Double> geoRiskMap = evaluator.geoRisk();

        Evaluator.printCountMap(bestModelMap, riskMap, ctiMap, zRiskMap, geoRiskMap);

        evaluator.riskSOTA();

        System.out.println("========= mean effective measures ===========");
        evaluator.printMeanWT();
        evaluator.printMean();

        System.out.println("=========  Random and Oracle ===========");
        System.out.println("Random : " + evaluator.random());
        System.out.println("RandomX : " + evaluator.randomX());
        System.out.println("Oracle : " + evaluator.oracleMax());
    }
}
