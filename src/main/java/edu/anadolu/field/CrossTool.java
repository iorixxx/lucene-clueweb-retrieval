package edu.anadolu.field;


import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import org.apache.commons.math3.stat.inference.TTest;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;

import static edu.anadolu.field.FieldTool.sortByValue;

/**
 * Comparison for across different index tags/folders (e.g. ICU, Latin, Standard, UAX)
 */
public class CrossTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "the collection", metaVar = "CW09A")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Cross-index comparison utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;

    @Option(name = "-tags", metaVar = "[ICU_Latin]", required = false, usage = "Index Tag")
    protected String tags = "ICU_Latin";

    @Option(name = "-baseline", required = false, usage = "term-weighting models")
    protected String baseline = "Latin";


    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url]", required = false, usage = "field that you want to search on")
    protected String fields = "title,body,url,description,keywords";

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;

    @Option(name = "-v", aliases = "--verbose", usage = "verbose=true: prints effectiveness scores for copy-paste")
    private boolean verbose = false;

    private final TTest tTest = new TTest();

    /**
     * Terrier's default values
     */
    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0_DPH_DFRee_DLH13_DFIC";

    @Override
    public void run(Properties props) throws Exception {

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        List<InfoNeed> needs = new ArrayList<>();
        Map<String, Evaluator> evaluatorMap = new HashMap<>();

        final String[] tagsArr = tags.split("_");

        Set<String> modelIntersection = new HashSet<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, models, evalDirectory, op);
            evaluatorMap.put(tag, evaluator);
            needs = evaluator.getNeeds();

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
        }

        Map<String, double[]> baselines = new HashMap<>();

        for (String model : modelIntersection) {

            double[] baseline = new double[needs.size()];

            for (int i = 0; i < needs.size(); i++)
                baseline[i] = evaluatorMap.get(this.baseline).score(needs.get(i), model);

            baselines.put(model, baseline);
        }


        for (String model : modelIntersection) {

            List<ModelScore> list = new ArrayList<>();

            for (String tag : tagsArr) {

                double[] scores = new double[needs.size()];

                final Evaluator evaluator = evaluatorMap.get(tag);

                for (int i = 0; i < needs.size(); i++)
                    scores[i] = evaluator.score(needs.get(i), model);

                ModelScore modelScore = evaluator.averagePerModel(model);

                if (tTest.pairedTTest(baselines.get(model), scores, 0.05)) {
                    list.add(new ModelScore(tag + "*", modelScore.score));

                    if (!verbose) continue;

                    // for risk graphs bar(sort(x(1,:)-x(2,:)))
                    System.out.print(baseline + "_" + model);
                    for (int i = 0; i < needs.size(); i++) {
                        System.out.print("\t" + baselines.get(model)[i]);
                    }
                    System.out.println();

                    System.out.print(tag + "_" + model);
                    for (int i = 0; i < needs.size(); i++) {
                        System.out.print("\t" + scores[i]);
                    }
                    System.out.println();

                } else
                    list.add(new ModelScore(tag, modelScore.score));

            }

            Collections.sort(list);

            if (needs.size() < 100)
                System.out.print(model + "(" + needs.size() + ") \t");
            else
                System.out.print(model + "(" + needs.size() + ")\t");

            for (ModelScore modelScore : list)
                System.out.print(modelScore.model + "(" + String.format("%.5f", modelScore.score) + ")\t");

            System.out.println();

        }

        System.out.println("========= oracles ==============");
        // if (!collection.equals(GOV2)) fields += ",anchor";

        for (String model : modelIntersection) {

            List<Prediction> list = new ArrayList<>(needs.size());

            Map<String, Integer> countMap = new HashMap<>();
            for (String tag : tagsArr) {
                countMap.put(tag, 0);
            }

            for (InfoNeed need : needs) {

                double max = Double.NEGATIVE_INFINITY;

                String bestTag = null;

                for (String tag : tagsArr) {

                    final Evaluator evaluator = evaluatorMap.get(tag);

                    double score = evaluator.score(need, model);
                    if (score > max) {
                        max = score;
                        bestTag = tag;
                    }
                }

                if (null == bestTag) throw new RuntimeException("bestField is null!");

                Prediction prediction = new Prediction(need, bestTag, max);
                list.add(prediction);

                Integer count = countMap.get(bestTag);
                countMap.put(bestTag, count + 1);

            }

            Solution solution = new Solution(list, -1);
            System.out.print(String.format("%s\tOracle(%.5f) \t", Evaluator.prettyModel(model), solution.getMean()));

            countMap = sortByValue(countMap);
            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                System.out.print(String.format("%s(%d | %.5f) \t",
                        entry.getKey(),
                        entry.getValue(),
                        evaluatorMap.get(entry.getKey()).averagePerModel(model).score));
            }
            System.out.println();
        }
    }
}
