package edu.anadolu.cmdline;

import edu.anadolu.QueryBank;
import edu.anadolu.QuerySelector;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.RBEvaluator;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tool for solely Rule Based (RB) approach
 */
public final class RBTool extends EvaluatorTool {

    @Option(name = "-ratio", metaVar = "[df|tf|cti]", required = false, usage = "Ratio metric for two term queries")
    private String ratio = "df";

    @Option(name = "-rb", required = false, usage = "Include Rule Based (RB) Approach")
    private boolean rb = false;

    @Override
    public String getShortDescription() {
        return "Tool for Rule Based (RB) approach";
    }

    /**
     * Removes queries having relevant documents less than the given threshold
     */
    private void applyMinRelConstraint() {

        Iterator<InfoNeed> iterator = needs.iterator();

        while (iterator.hasNext()) {
            InfoNeed need = iterator.next();

            if (need.relevant() < minRel) iterator.remove();
        }
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        QueryBank bank = new QueryBank(dataset);

        // queries having lambda greater than one
        if (-1 == query) {
            needs = new QuerySelector(dataset, tag).lambdaQueries(e);
        } else if (-2 == query)
            needs = new QuerySelector(dataset, tag).remaining(e);
        else if (-5 == query)
            needs = new QuerySelector(dataset, tag).nonDecreasingQueries(e);
        else if (0 == query)
            needs = bank.getAllQueries(minRel);
        else
            needs = bank.getQueries(minRel, query);

        applyMinRelConstraint();


        RBEvaluator evaluator = new RBEvaluator(dataset, tag, measure, models, "evals", op);
        evaluator.setRatio(ratio);

        evaluator.printSortedTopicModel();
        Map<String, List<InfoNeed>> bestModelMap = evaluator.bestModelMap();
        Map<String, Double> riskMap = evaluator.riskSOTA();
        Map<String, Double> ctiMap = evaluator.cti();
        Map<String, Double> zRiskMap = evaluator.zRisk();
        Map<String, Double> geoRiskMap = evaluator.geoRisk();

        Evaluator.printCountMap(bestModelMap, riskMap, ctiMap, zRiskMap, geoRiskMap);

        evaluator.riskSOTA();


        if (query == 1) {
            System.out.println("========= spam documents ===========");
            evaluator.printVerboseBestOTQ(bestModelMap);
        }
        if (query == -5) {
            System.out.println("========= max bin ===========");
            evaluator.printVerboseBestNonDecreasing(bestModelMap);
        } else {
            System.out.println("========= discriminative ratio scores ===========");
            evaluator.printVerboseBestTopics(bestModelMap);
        }

        System.out.println("========= mean effective measures ===========");
        evaluator.printMeanWT();
        evaluator.printMean();

        System.out.println("=========  Random and Oracle ===========");
        System.out.println("Random : " + evaluator.random());
        System.out.println("RandomX : " + evaluator.randomX());
        System.out.println("Oracle : " + evaluator.oracleMax());


        if (rb && evaluator.getModelSet().contains("RawTF") && evaluator.getModelSet().contains("LogTFNv0L0")) {

            System.out.println("=========  RuleBased and Oracles ===========");
            System.out.println("Rule Based Approach : " + evaluator.RB(e));
            System.out.println("Global Oracle  : " + evaluator.oracleMax());

            System.out.println("=========  Wilcoxon and TTest's P values ===========");
            evaluator.displayPValues(e);
        }

    }
}
