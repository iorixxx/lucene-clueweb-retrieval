package edu.anadolu.field;


import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import org.apache.commons.math3.stat.inference.TTest;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;

/**
 * Comparison for across different index tags/folders (e.g. ICU, Latin, Standard, UAX)
 */
public class CrossTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
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

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "all";


    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url]", required = false, usage = "field that you want to search on")
    protected String fields = "title,body,url,description,keywords";

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;

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

        final String[] tagsArr = tags.split("_");

        for (String tag : tagsArr) {
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, models, evalDirectory, op);
            evaluatorMap.put(tag, evaluator);
            needs = evaluator.getNeeds();
        }

        Map<String, double[]> baselines = new HashMap<>();

        for (String model : "DPH_DFIC_DFRee_DLH13".split("_")) {

            double[] baseline = new double[needs.size()];

            for (int i = 0; i < needs.size(); i++)
                baseline[i] = evaluatorMap.get("Latin").score(needs.get(i), model);

            baselines.put(model, baseline);
        }


        for (String model : "DPH_DFIC_DFRee_DLH13".split("_")) {

            List<ModelScore> list = new ArrayList<>();

            for (String tag : tagsArr) {

                double[] scores = new double[needs.size()];

                final Evaluator evaluator = evaluatorMap.get(tag);

                for (int i = 0; i < needs.size(); i++)
                    scores[i] = evaluator.score(needs.get(i), model);

                ModelScore modelScore = evaluator.averagePerModel(model);

                if (tTest.pairedTTest(baselines.get(model), scores, 0.05))
                    list.add(new ModelScore(tag + "*", modelScore.score));
                else
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

    }
}
