package edu.anadolu.knn;

import edu.anadolu.eval.ModelScore;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a solution: saves predicted models and their effectiveness measures
 */
public final class Solution implements Comparable<Solution>, Cloneable {

    public final List<Prediction> list;

    public double sigma1;
    public double sigma0;

    public int hits2;
    public int hits1;
    public int hits0;

    final double[] scores;

    public double geoRisk;

    public double[] scores() {
        return this.scores;
    }

    final double mean;

    public double getMean() {
        return mean;
    }


    String runLabel;

    public void setRunLabel(String runLabel) {
        this.runLabel = runLabel;
    }

    public String key;

    public void setKey(String key) {
        this.key = key;
    }

    public String getRunLabel() {
        return runLabel;
    }

    double improvement;

    public void calculateImprovement(ModelScore best, ModelScore oracle) {
        this.improvement = (mean - best.score) / (oracle.score - best.score) * 100d;
    }

    @Override
    public int compareTo(Solution s) {
        return (int) Math.signum(s.mean - this.mean);
    }

    @Override
    public String toString() {
        return key + "\t" + String.format("%.2f", sigma0) + "\t" + String.format("%.2f", sigma1) + "\t" + String.format("%.5f", mean);
    }

    public Solution(List<Prediction> list, int k) {
        this.list = list;
        this.k = k;
        this.scores = new double[list.size()];
        double mean = 0.0;

        int c = 0;
        for (Prediction prediction : list) {
            scores[c] = prediction.predictedScore;
            c++;
            mean += prediction.predictedScore;
        }
        this.mean = mean / (double) list.size();
    }


    public final int k;
    public String voter;

    public String tie;
    public String model;
    public String agg;

    public Relax relax;

    public Predict predict;


    public String attr(String attr) {

        switch (attr) {
            case "MODEL":
                return model;

            case "AGG":
                return agg;

            case "RELAX":
                return relax.toString();

            case "PRED":
                return predict.toString();

            case "TIE":
                return tie;

            default:
                throw new AssertionError("unknown attribute " + attr);
        }
    }


    @Override
    public Solution clone() {

        Solution foo;
        try {
            foo = (Solution) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error();
        }

        // Deep clone member fields here

        return foo;
    }

    private Solution(double improvement, String voter, String runLabel, int k, double mean, double accuracy) {
        this.improvement = improvement;
        this.voter = voter;
        this.runLabel = runLabel;
        this.k = k;
        this.mean = mean;
        this.sigma1 = accuracy;
        this.list = Collections.emptyList();
        this.scores = new double[1200];
    }

    /**
     * i=0	***1.58
     * i=1	NaiveVoter_KStemAnalyzerAnchor
     * i=2	Average_ChiSquarel=1u=250divide=truecdf=false
     * i=3	k=13
     * i=4	ERR=0.14802
     * i=5	sigma1=15.38
     * i=6	multiAcc=28.57
     */
    public static Solution parseSolution(String[] parts) {
        return new Solution(
                Double.parseDouble(parts[0].substring(3)),
                parts[1],
                parts[2],
                Integer.parseInt(parts[3].substring(2)),
                doubleAfterEquals(parts[4]),
                doubleAfterEquals(parts[5])
        );
    }

    private static double doubleAfterEquals(String s) {
        int i = s.indexOf("=");

        if (i == -1) throw new RuntimeException("cannot find = inside the string : " + s);

        return Double.parseDouble(s.substring(i + 1));
    }
}
