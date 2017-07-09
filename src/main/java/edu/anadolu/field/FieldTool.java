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
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;

import static edu.anadolu.datasets.Collection.GOV2;

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

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "DPH_DFIC_DFRee_DLH13";


    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url]", required = false, usage = "field that you want to search on")
    protected String fields = "title,body,url,description,keywords";

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        List<InfoNeed> needs = new ArrayList<>();
        Map<String, Evaluator> evaluatorMap = new HashMap<>();

        final String[] fieldsArr = props.getProperty(collection.toString() + ".fields", "description,keywords,title,body,anchor,url").split(",");

        for (String field : fieldsArr) {
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, models, evalDirectory, op, field);
            evaluatorMap.put(field, evaluator);
            needs = evaluator.getNeeds();
        }


        for (String model : models.split("_")) {

            List<ModelScore> list = new ArrayList<>();

            for (String field : fieldsArr) {

                final Evaluator evaluator = evaluatorMap.get(field);

                ModelScore modelScore = evaluator.averagePerModel(model);
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

       // if (!collection.equals(GOV2)) fields += ",anchor";

        for (String model : models.split("_")) {

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
            System.out.print(String.format("%s(oracle) \t %.5f \t", model, solution.getMean()));

            for (String field : fields.split(",")) {
                System.out.print(field + "(" + countMap.get(field) + ")\t");
            }

            System.out.println();

        }
    }
}
