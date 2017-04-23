package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.exp.Presentation;
import edu.anadolu.knn.Measure;
import org.kohsuke.args4j.Option;

import java.util.*;

/**
 * List average effectiveness of models at different spam threshold cut-offs
 */
public final class SpamEvalTool extends EvaluatorTool {


    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;


    @Option(name = "-agg", required = true, usage = "aggregation")
    private AGG agg;


    public enum AGG {
        M,
        E,
        G
    }

    @Override
    public String getShortDescription() {
        return "List average effectiveness of models at different spam threshold cut-offs";
    }

    /**
     * Prints spam sensitivity of term-weighting models
     *
     * @param map
     */
    private void display(SortedMap<Integer, List<ModelScore>> map) {

        List<String> models = new ArrayList<>();

        Map<String, List<Double>> scores = new HashMap<>();

        for (int t : map.keySet()) {
            List<ModelScore> list = map.get(t);

            for (ModelScore modelScore : list) {
                if (scores.containsKey(modelScore.model)) {
                    scores.get(modelScore.model).add(modelScore.score);
                } else {
                    List<Double> doubleList = new ArrayList<>();
                    doubleList.add(modelScore.score);
                    scores.put(modelScore.model, doubleList);
                }
            }

            list.sort((o1, o2) -> o1.model.compareTo(o2.model));
            list.forEach(modelScore -> System.out.print(modelScore.score + "\t"));

            if (t == 0)
                list.forEach(modelScore -> models.add(Evaluator.prettyModel(modelScore.model)));

            System.out.println();
        }

        map.keySet().forEach(i -> System.out.print(i + "\t"));

        System.out.println();
        models.forEach(System.out::println);

        for (String model : scores.keySet()) {
            String s = String.format("%s \t %.1f", Evaluator.prettyModel(model), Presentation.var4(scores.get(model).stream().mapToDouble(d -> d).toArray()));
            System.out.println(s);
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

        SortedMap<Integer, List<ModelScore>> map = new TreeMap<>();

        Evaluator evaluator = new Evaluator(dataset, tag, measure, "all", "evals", op);
        final String models = evaluator.models();

        int maxSpam = 0;
        double max = evaluator.averageOfAllModels(agg);

        System.out.print(String.format("%.5f", max) + "\tspamThreshold = 0\t");
        evaluator.printMean();
        map.put(0, evaluator.averageForAllModels());
        System.out.println("=======================");

        for (int spamThreshold = 5; spamThreshold <= 95; spamThreshold += 5) {

            evaluator = new Evaluator(dataset, tag, measure, models, "spam_" + spamThreshold + "_evals", op);

            double mean = evaluator.averageOfAllModels(agg);

            System.out.print(String.format("%.5f", mean) + "\tspamThreshold = " + spamThreshold + "\t");
            evaluator.printMean();
            map.put(spamThreshold, evaluator.averageForAllModels());
            System.out.println("=======================");

            if (mean > max) {
                max = mean;
                maxSpam = spamThreshold;
            }
        }

        System.out.println("================= Best threshold is " + maxSpam + " =======================" + "Aggregated with " + agg);

        if ("bkk".equals(task))
            display(map);
    }

    static String cacheKey(String tag, Measure measure, String op) {
        return tag + "_" + measure.metric().toString() + "_" + measure.k() + "_" + op;
    }

    public static int bestSpamThreshold(DataSet dataset, String tag, Measure measure, String op) {

        if (Collection.GOV2.equals(dataset.collection()) || Collection.MC.equals(dataset.collection()) || Collection.ROB04.equals(dataset.collection())) {
            throw new RuntimeException("GOV2, ROB04, and MC do not have spam filtering option!");
        }

        String key = cacheKey(tag, measure, op);

        Properties properties = cacheProperties(dataset);

        if (properties.getProperty(key) != null) {
            String value = properties.getProperty(key);
            return Integer.parseInt(value);
        }


        Evaluator evaluator = new Evaluator(dataset, tag, measure, "all", "evals", op);
        final String models = evaluator.models();

        int maxSpam = 0;
        double max = evaluator.averageOfAllModels();


        for (int spamThreshold = 5; spamThreshold <= 95; spamThreshold += 5) {

            evaluator = new Evaluator(dataset, tag, measure, models, "spam_" + spamThreshold + "_evals", op);

            double mean = evaluator.averageOfAllModels();


            if (mean > max) {
                max = mean;
                maxSpam = spamThreshold;
            }
        }

        properties.put(key, Integer.toString(maxSpam));
        saveCacheProperties(dataset, properties);

        return maxSpam;
    }
}
