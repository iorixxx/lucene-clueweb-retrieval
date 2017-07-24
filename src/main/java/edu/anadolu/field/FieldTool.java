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
import java.util.stream.Collectors;

/**
 * Field based scoring tool
 */
public class FieldTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Field-based scoring utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;

    @Option(name = "-tag", metaVar = "[KStemField|KStem]", required = false, usage = "Index Tag")
    protected String tag = "KStemField";


    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url]", required = false, usage = "field that you want to search on")
    protected String fields = "title,body,url,description,keywords";

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

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        List<InfoNeed> needs = new ArrayList<>();
        Map<String, Evaluator> evaluatorMap = new HashMap<>();

        final String[] fieldsArr = props.getProperty(collection.toString() + ".fields", "description,keywords,title,body,anchor,url").split(",");

        Set<String> modelIntersection = new HashSet<>();

        for (int i = 0; i < fieldsArr.length; i++) {
            String field = fieldsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "DPH_DFIC_DFRee_DLH13", evalDirectory, op, field);
            evaluatorMap.put(field, evaluator);
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
                baseline[i] = evaluatorMap.get("bt").score(needs.get(i), model);

            baselines.put(model, baseline);
        }


        for (String model : modelIntersection) {

            List<ModelScore> list = new ArrayList<>();

            for (String field : fieldsArr) {

                double[] scores = new double[needs.size()];

                final Evaluator evaluator = evaluatorMap.get(field);

                for (int i = 0; i < needs.size(); i++)
                    scores[i] = evaluator.score(needs.get(i), model);

                ModelScore modelScore = evaluator.averagePerModel(model);

                if (tTest.pairedTTest(baselines.get(model), scores, 0.05))
                    list.add(new ModelScore(field + "*", modelScore.score));
                else
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

        }

        System.out.println("========= oracles ==============");
        // if (!collection.equals(GOV2)) fields += ",anchor";

        for (String model : modelIntersection) {

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
