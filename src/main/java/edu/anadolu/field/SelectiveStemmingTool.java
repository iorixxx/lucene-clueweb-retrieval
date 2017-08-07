package edu.anadolu.field;

import edu.anadolu.QuerySelector;
import edu.anadolu.SelectionMethods;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
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
import edu.anadolu.stats.TermStats;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.lucene.analysis.Analyzer;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;

import static edu.anadolu.field.FieldTool.sortByValue;


public class SelectiveStemmingTool extends CmdLineTool {
    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "Zemberek_NoStemTurkish")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Selective Stemming utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.MAP;

    @Option(name = "-tags", metaVar = "[NoStemTurkish_Zemberek|NoStem_KStem]", required = false, usage = "Index Tag")
    protected String tags = "NoStemTurkish_Zemberek";

    protected String baseline;

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url|contents]", required = false, usage = "field that you want to search on")
    protected String fields = "contents";

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
        Map<String, QuerySelector> querySelectorMap = new HashMap<>();

        final String[] tagsArr = tags.split("_");
        if(tagsArr.length!=2) return;
        baseline=tagsArr[0];

        Set<String> modelIntersection = new HashSet<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
            evaluatorMap.put(tag, evaluator);
            needs = evaluator.getNeeds();

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
            querySelectorMap.put(tag, new QuerySelector(dataSet, tag));
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

                if (tTest.pairedTTest(baselines.get(model), scores, 0.05))
                    list.add(new ModelScore(tag + "*", modelScore.score));
                else
                    list.add(new ModelScore(tag, modelScore.score));

            }

            Solution solution = selectiveStemmingSolution(model,evaluatorMap,querySelectorMap,needs,tagsArr);
            if (tTest.pairedTTest(baselines.get(model), solution.scores(), 0.05))
                list.add(new ModelScore("SelectiveStemming" + "*", solution.getMean()));
            else
                list.add(new ModelScore("SelectiveStemming", solution.getMean()));

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
            System.out.print(String.format("%s(%.5f) \t", model, solution.getMean()));

            countMap = sortByValue(countMap);
            for (Map.Entry<String, Integer> entry : countMap.entrySet())
                System.out.print(entry.getKey() + "(" + entry.getValue() + ")\t");

            System.out.println();
        }
    }

    private Solution selectiveStemmingSolution(String model,Map<String, Evaluator> evaluatorMap,Map<String,
            QuerySelector> querySelectorMap,List<InfoNeed> needs, String[] tagsArr){

        List<Prediction> list = new ArrayList<>(needs.size());

        for (InfoNeed need : needs) {
            String predictedTag = null;
            Map<String,ArrayList<TermStats>> tagTermStatsMap = new LinkedHashMap<>();
            for (String tag : tagsArr) {
                QuerySelector selector = querySelectorMap.get(tag);
                Map<String, TermStats> statsMap = selector.termStatisticsMap;
               // Evaluator evaluator = evaluatorMap.get(tag);
                String query = need.query();
                Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));
                for (String term : Analyzers.getAnalyzedTokens(query, analyzer)) {
                    TermStats termStats = statsMap.get(term);
                    if (termStats == null) {
                        termStats = new TermStats(term,0,0,0);//indexes do not contain query term
                        //throw new RuntimeException("Term stats cannot be null: "+ term );
                    }
                    ArrayList<TermStats> termStatsList=tagTermStatsMap.get(tag);
                    if(termStatsList==null){
                        ArrayList<TermStats> l = new ArrayList<>();
                        l.add(termStats);
                        tagTermStatsMap.put(tag,l);
                    }else termStatsList.add(termStats);

                    //System.out.println(tag + " " + term + " " + termStats.totalTermFreq() + " " + termStats.docFreq());
                   // double score = evaluator.score(need, model);
                   // System.out.println(tag + " " + need.id() + " " + score);
                }
            }
            predictedTag = SelectionMethods.MSTTermFreq(tagTermStatsMap,tagsArr);
            double predictedScore = evaluatorMap.get(predictedTag).score(need, model);
            Prediction prediction = new Prediction(need, predictedTag, predictedScore);
            list.add(prediction);

        }
        Solution solution = new Solution(list, -1);
        System.out.print(String.format("%s(%.5f) \t", model, solution.getMean()));
        return solution;
    }
}
