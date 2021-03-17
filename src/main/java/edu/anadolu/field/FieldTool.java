package edu.anadolu.field;

import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CustomTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Field based scoring tool
 */
public class FieldTool extends CustomTool {


    @Override
    public String getShortDescription() {
        return "Field-based scoring utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;

    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private final TTest tTest = new TTest();

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        this.models = this.models + "_DPH_DFIC";
        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        List<InfoNeed> needs = new ArrayList<>();
        Map<String, Evaluator> evaluatorMap = new HashMap<>();

        if (!props.containsKey(collection.toString() + ".fields"))
            throw new RuntimeException("cannot find " + collection.toString() + ".fields property!");

        final String[] fieldsArr = props.getProperty(collection.toString() + ".fields").split(",");

        Set<String> models = Arrays.stream(this.models.split("_")).collect(Collectors.toSet());

        for (int i = 0; i < fieldsArr.length; i++) {
            String field = fieldsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag.toString(), measure, this.models, evalDirectory, op, field);
            evaluatorMap.put(field, evaluator);
            needs = evaluator.getNeeds();
        }

        Map<String, double[]> baselines = new HashMap<>();

        for (String model : models) {

            double[] baseline = new double[needs.size()];

            for (int i = 0; i < needs.size(); i++)
                baseline[i] = evaluatorMap.get("bt").score(needs.get(i), model);

            baselines.put(model, baseline);
        }

        for (String model : models) {
            final double meanBaseLine = StatUtils.mean(baselines.get(model));

            List<ModelScore> list = new ArrayList<>();

            for (String field : fieldsArr) {

                double[] scores = new double[needs.size()];

                final Evaluator evaluator = evaluatorMap.get(field);

                for (int i = 0; i < needs.size(); i++)
                    scores[i] = evaluator.score(needs.get(i), model);

                ModelScore modelScore = evaluator.averagePerModel(model);

                if (tTest.pairedTTest(baselines.get(model), scores, 0.05)) {

                    if (modelScore.score > meanBaseLine)
                        list.add(new ModelScore(field + "*", modelScore.score));
                    else
                        list.add(new ModelScore(field + "+", modelScore.score));

                } else
                    list.add(new ModelScore(field, modelScore.score));
            }

            Collections.sort(list);

            if (needs.size() < 100)
                System.out.print(model + "(" + needs.size() + ") \t");
            else
                System.out.print(model + "(" + needs.size() + ")\t");

            for (ModelScore modelScore : list)
                System.out.print(modelScore.model + "(" + String.format("%.5f", modelScore.score) + ")\t");

            System.out.println();

            for (String mode : new String[]{"bt", "btd", "btk", "btdk"})
                for (ModelScore modelScore : list)
                    if (modelScore.model.equals(mode) || modelScore.model.equals(mode + "*") || modelScore.model.equals(mode + "+"))
                        System.err.println(collection + "," + measure + "," + Evaluator.prettyModel(model) + "," + modelScore.model + "," + String.format("%.5f", modelScore.score));

        }

        System.out.println("========= oracles ==============");

        final String fields =
                (collection.equals(Collection.MQ07) || collection.equals(Collection.MQ08) || collection.equals(Collection.GOV2)) ?
                        "title,body,url,description,keywords" : "title,body,url,description,keywords,anchor";

        for (
                String model : models) {

            List<Prediction> list = new ArrayList<>(needs.size());

            Map<String, Integer> countMap = new HashMap<>();
            for (String field : fields.split(",")) {
                countMap.put(field, 0);
            }

            for (InfoNeed need : needs) {

                double max = Double.NEGATIVE_INFINITY;

                String bestField = null;

                for (String field : fields.split(",")) {

                    final Evaluator evaluator = evaluatorMap.get(field);

                    double score = evaluator.score(need, model);
                    if (score > max) {
                        max = score;
                        bestField = field;
                    }
                }

                if (null == bestField) throw new RuntimeException("bestField is null!");

                Prediction prediction = new Prediction(need, bestField, max);
                list.add(prediction);

                Integer count = countMap.get(bestField);
                countMap.put(bestField, count + 1);

            }

            Solution solution = new Solution(list, -1);
            System.out.print(String.format("%s(%.5f) \t", model, solution.getMean()));

            countMap = sortByValue(countMap);
            for (Map.Entry<String, Integer> entry : countMap.entrySet())
                System.out.print(entry.getKey() + "(" + entry.getValue() + ")\t");

            System.out.println();
        }
    }
}
