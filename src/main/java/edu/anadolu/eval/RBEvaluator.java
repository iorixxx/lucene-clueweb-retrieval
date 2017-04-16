package edu.anadolu.eval;

import edu.anadolu.QuerySelector;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import org.apache.commons.math3.stat.StatUtils;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.util.*;

/**
 * Evaluator for Rule Based (RB) approach
 */
public class RBEvaluator extends Evaluator {

    private final QuerySelector selector;

    private String ratio = "df";

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    private final DataSet dataSet;

    public RBEvaluator(DataSet dataSet, String indexTag, Measure measure, String models, String evalDirectory, String op) {
        super(dataSet, indexTag, measure, models, evalDirectory, op);
        this.selector = new QuerySelector(dataSet, indexTag);
        this.dataSet = dataSet;
    }


    /**
     * Main entry to our Rule Based (RB) approach
     *
     * @param e expected under average document length
     * @return Score of rule based strategy
     */
    private double scoreRuleBased(InfoNeed need, double e) {

        int query_length = need.wordCount();

        if (query_length == 1) return score(need, "RawTF");
        if (selector.isLambdaGreaterThan(need, e)) return score(need, "DFIC");
        if (query_length == 2) {

            double p = selector.termRatio(need, ratio);

            if (p < 2.0)
                return score(need, "LogTFNv0L0");
            else
                return score(need, "DPH");
        }

        return score(need, "DFIC");
    }

    /**
     * Rule Based Approach
     *
     * @param e expected under average document length
     * @return Score array of the rule based strategy
     */
    private double[] scoreArrayRuleBased(double e) {

        double scores[] = new double[needs.size()];
        Arrays.fill(scores, 0.0);

        int c = 0;
        for (InfoNeed need : needs) {
            scores[c++] = scoreRuleBased(need, e);
        }

        return scores;
    }

    private ModelScore hybrid(String fallBackModel, double threshold) throws IOException {

        double mean = 0.0;

        for (InfoNeed need : needs) {

            double p = selector.termRatio(need, ratio);

            if (p < threshold)
                mean += score(need, "DFIC");
            else
                mean += score(need, fallBackModel);

        }

        mean /= (double) needs.size();

        return new ModelScore("HYBRID" + fallBackModel + "(" + threshold + ")", mean);

    }

    /**
     * Rule-Based Approach: if term discriminative ratio is less than a given threshold
     * use LogTF; else fallback to DPH
     *
     * @param fallBackModel usually DPH
     * @param threshold     term discriminative ratio
     * @param wt            WebTrack
     * @return SimScore
     * @throws IOException
     */
    private ModelScore hybrid(String fallBackModel, double threshold, Track wt) throws IOException {

        double mean = 0.0;

        int counter = 0;
        for (InfoNeed need : needs) {

            if (!wt.equals(need.getWT())) continue;

            counter++;
            double p = selector.termRatio(need, ratio);

            if (p < threshold)
                mean += score(need, "DFIC");
            else
                mean += score(need, fallBackModel);

        }

        mean /= (double) counter;

        return new ModelScore("HYBRID" + fallBackModel + "(" + threshold + ")", mean);

    }

    public ModelScore RB(double e) throws IOException {
        return new ModelScore("RB", StatUtils.mean(scoreArrayRuleBased(e)));
    }

    public void displayPValues(double e) {
        final double[] rb = scoreArrayRuleBased(e);
        System.out.println("=== Rule Based Approach P Values ===");
        displayPValues(rb);


        final double[] globalOracle = oracleMinAsSolution().scores();
        System.out.println("=== Global Oracle P Values ===");
        displayPValues(globalOracle);


        double tP = tTest.pairedTTest(rb, globalOracle) / 2d;
        double wP = wilcoxonSignedRankTest.wilcoxonSignedRankTest(rb, globalOracle, false);
        System.out.println("RB vs GlobalOracle " + "(tp:" + String.format("%.5f", tP) + "; wp:" + String.format("%.5f", wP) + ")");
    }

    /**
     * print phraseness score too
     *
     * @param map map
     * @throws IOException
     */

    public void printVerboseBestNonDecreasing(Map<String, List<InfoNeed>> map) throws IOException {

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {
            List<NeedDouble> list = new ArrayList<>();
            for (InfoNeed need : entry.getValue()) {
                list.add(new NeedDouble(need, selector.maxBin(need)));
            }
            Collections.sort(list);
            System.out.println(entry.getKey() + "\t" + list);
        }
    }

    public void printVerboseBestOTQ(Map<String, List<InfoNeed>> map) throws IOException {

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {
            List<NeedDouble> list = new ArrayList<>();
            for (InfoNeed need : entry.getValue()) {
                list.add(new NeedDouble(need, need.spam()));
            }
            Collections.sort(list);
            Collections.reverse(list);
            System.out.println(entry.getKey() + "\t" + list);
        }
    }

    /**
     * print phraseness score too
     *
     * @throws IOException
     */

    public void printVerboseBestTopics(Map<String, List<InfoNeed>> map) throws IOException {

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {
            List<NeedDouble> list = new ArrayList<>();
            for (InfoNeed need : entry.getValue()) {
                list.add(new NeedDouble(need, selector.termRatio(need, ratio)));
            }
            Collections.sort(list);
            System.out.println(entry.getKey() + "\t" + list);
        }
    }

    private void intermediate(int query, double e, Measure measure, boolean first) throws IOException {

        RBEvaluator evaluator = new RBEvaluator(dataSet, "KStemAnalyzer", measure, models, "evals", "AND");

        List<ModelScore> all = evaluator.averageForAllModels(needs);

        String rawTF;

        if (query == 1) rawTF = "RawTF";
        else rawTF = "LogTFNv0L0";

        if (query == 0 || query == 2)
            all.add(evaluator.RB(e));


        all.add(evaluator.oracleMax());

        if (first) {

            System.out.println(tabular(all.size() + 1));
            System.out.println("\\hline");

            System.out.println(modelHeader(all));
            System.out.println("\\hline");
        }


        System.out.println(measure.toString() + "@" + k + " & " + singleLine(toDouble(all)));

        System.out.println("\\hline");

        System.out.println(evaluator.bestModelCounts(all));
        System.out.println("\\hline");

    }

    /**
     * Print mean effective measures
     *
     * @throws IOException
     */
    public void latex(int query, double e) throws IOException {

        System.out.println();

        String tableLabel = "err_ndcg" + k + indexTag + query;

        System.out.println("\\begin{table}");
        System.out.println("\\caption{ERR and NDCG at rank " + k + " for " + query + " term queries - " + needs.size() + "}");
        System.out.println("\\label{tbl:" + tableLabel + "}");
        System.out.println("\\centering");

        intermediate(query, e, Measure.ERR20, true);

        intermediate(query, e, Measure.NDCG20, false);

        intermediate(query, e, Measure.MAP, false);

        System.out.println("\\end{tabular}");
        System.out.println("\\end{table}");
        System.out.println();
    }

    String bestModelCounts(List<ModelScore> all) throws IOException {

        String sideInfo = "";

        if (bestModelMap.containsKey("ALL_ZERO"))
            sideInfo = sideInfo + "z=" + bestModelMap.get("ALL_ZERO").size() + ",";

        if (bestModelMap.containsKey("ALL_SAME"))
            sideInfo = sideInfo + "s=" + bestModelMap.get("ALL_SAME").size() + ",";

        if (sideInfo.endsWith(",")) sideInfo = sideInfo.substring(0, sideInfo.length() - 1);

        StringBuilder builder = new StringBuilder();

        builder.append("Top (").append(sideInfo).append(") & ");

        for (ModelScore modelScore : all) {

            if (bestModelMap.containsKey(modelScore.model))
                builder.append(bestModelMap.get(modelScore.model).size());
            else if (modelScore.model.toLowerCase(Locale.US).contains("oracle"))
                builder.append(" - ");
            else if (modelScore.model.startsWith("RB"))
                builder.append(" - ");
            else
                builder.append(" 0 ");

            builder.append(" & ");
        }


        lastAmpersand(builder);

        return builder.toString();

    }


    String modelHeader(List<ModelScore> all) {
        StringBuilder builder = new StringBuilder(" & ");

        for (ModelScore modelScore : all)
            builder.append(prettyModel(modelScore.model)).append(" & ");

        lastAmpersand(builder);

        return builder.toString();

    }


    String singleLine(List<Double> array) {

        double max = Collections.max(array);

        StringBuilder builder = new StringBuilder();

        for (double d : array)
            if (d == max)
                builder.append("\\textbf{").append(String.format("%.5f", d)).append("} & ");
            else
                builder.append(String.format("%.5f", d)).append(" & ");

        lastAmpersand(builder);
        return builder.toString();

    }

    /**
     * delete last ampersand and append latex table line ending
     *
     * @param builder builder
     */
    void lastAmpersand(StringBuilder builder) {
        builder.delete(builder.length() - 3, builder.length());
        builder.append(" \\\\");
    }

    String tabular(int n) {
        StringBuilder builder = new StringBuilder("\\begin{tabular}{|");
        for (int i = 0; i < n; i++) {
            builder.append(" c |");

        }
        builder.append("}");

        return builder.toString();
    }

}
