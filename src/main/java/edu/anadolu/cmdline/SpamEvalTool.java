package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.TStats;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.kohsuke.args4j.Option;

import java.util.*;
import java.util.stream.Collectors;

/**
 * List average effectiveness of models at different spam threshold cut-offs
 */
public final class SpamEvalTool extends EvaluatorTool {

    @Option(name = "-i", required = false, usage = "increments of spam threshold", metaVar = "5 10 20")
    private int i = 5;


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
    static void display(SortedMap<Integer, List<ModelScore>> map) {

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

            list.sort(Comparator.comparing(ModelScore::model));
            list.forEach(modelScore -> System.out.print(modelScore.score + "\t"));

            if (t == 0)
                list.forEach(modelScore -> models.add(Evaluator.prettyModel(modelScore.model)));

            System.out.println();
        }

        map.keySet().forEach(i -> System.out.print(i + "\t"));

        System.out.println();
        models.forEach(System.out::println);

        print(scores.keySet()
                .stream()
                .map(s -> new ModelScore(s, coefficientOfVariation(scores.get(s).stream().mapToDouble(d -> d).toArray())))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList())
        );

    }

    /**
     * http://www.statisticshowto.com/probability-and-statistics/how-to-find-a-coefficient-of-variation/
     * The coefficient of variation (CV) is a measure of relative variability.
     *
     * @param array data elements
     * @return coefficient of variation, which is the ratio of the standard deviation to the mean (average).
     */
    public static double coefficientOfVariation(double[] array) {

        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (double d : array)
            stats.addValue(d);

        return stats.getStandardDeviation() / stats.getMean() * 100;

    }

    static private void print(List<ModelScore> list) {
        System.out.println("=========================");
        for (ModelScore m : list) {
            String s = String.format("%s \t %.2f", Evaluator.prettyModel(m.model), m.score);
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

        if (!dataset.spamAvailable()) {
            System.out.println(dataset.toString() + " do not have spam filtering option!");
            return;
        }

        SortedMap<Integer, List<ModelScore>> map = new TreeMap<>();
        SortedMap<Integer, Struct> tRisk = new TreeMap<>();

        Evaluator evaluator = new Evaluator(dataset, tag, measure, "all", "evals", op);
        final String models = evaluator.models();

        int maxSpam = 0;
        double max = evaluator.averageOfAllModels(agg);

        System.out.print(String.format("%.5f", max) + "\tspamThreshold = 0\t");
        evaluator.printMean();
        map.put(0, evaluator.averageForAllModels());
        tRisk.put(0, new Struct(evaluator.scoreArray("DPH"), evaluator.scoreArray("DFIC")));
        System.out.println("=======================");

        for (int spamThreshold = i; spamThreshold < 100; spamThreshold += i) {

            evaluator = new Evaluator(dataset, tag, measure, models, "spam_" + spamThreshold + "_evals", op);

            double mean = evaluator.averageOfAllModels(agg);

            tRisk.put(spamThreshold, new Struct(evaluator.scoreArray("DPH"), evaluator.scoreArray("DFIC")));

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

        if ("bkk".equals(task)) {
            display(map);

            Struct base = tRisk.get(0);

            for (int spamThreshold = i; spamThreshold < 100; spamThreshold += i) {

                Struct struct = tRisk.get(spamThreshold);

                double DPH = TStats.tRisk(base.DPH, struct.DPH, 5);
                double DFI = TStats.tRisk(base.DFI, struct.DFI, 5);

                System.out.println(spamThreshold + "\t" + String.format("%.5f", DPH) + "\t" + String.format("%.5f", DFI));
            }
        }
    }



    static class Struct {

        final double[] DPH;
        final double[] DFI;

        Struct(double[] DPH, double[] DFI) {
            this.DFI = DFI;
            this.DPH = DPH;
        }
    }

    static String cacheKey(String tag, Measure measure, String op) {
        return tag + "_" + measure.metric().toString() + "_" + measure.k() + "_" + op;
    }

    public static int bestSpamThreshold(DataSet dataset, String tag, Measure measure, String op) {

        if (!dataset.spamAvailable()) {
            throw new RuntimeException(dataset.toString() + " do not have spam filtering option!");
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
