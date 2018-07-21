package edu.anadolu.knn;

import org.apache.commons.math3.stat.StatUtils;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Considers cartesian product of queries' terms
 */
public class CartesianQueryTermSimilarity extends QuerySimilarityBase {

    /**
     * We have examined several aggregation methods that could serve the purpose, including
     * arithmetic mean, median, geometric mean, Euclidean distance, minimum value, maximum value, ratio of the minimum to maximum, log-ratio, log-odds, etc. The aggregation method that serves well with respect to both average effectiveness and robustness is the Normalized Euclidean Distance, as given by:
     */
    public enum Aggregation {
        Euclid, Ari, Geo
    }

    public enum Way {
        Mean,
        m,
        s,
        disjunctionMax,
        x
    }

    public String aggregation() {
        return agg.toString().charAt(0) + "_" + way.toString().charAt(0);
    }

    private final Aggregation agg;
    protected final Way way;

    public CartesianQueryTermSimilarity(ChiBase chi, boolean zero, Aggregation agg, Way way) {
        super(chi, zero);
        this.agg = agg;
        this.way = way;
    }

    /**
     * @param R Test Query
     * @param S Training Query
     * @return similarity
     */
    @Override
    public double score(TFDAwareNeed R, TFDAwareNeed S) {

        if (zero)
            return scoreX(R.termFreqDistZeroNormalized, S.termFreqDistZeroNormalized);
        else
            return scoreX(R.termFreqDistNormalized, S.termFreqDistNormalized);
    }

    protected static class Entry implements Comparable<Entry> {
        final int i;
        final int j;
        final double similarity;

        public Entry(int i, int j, double similarity) {
            this.i = i;
            this.j = j;
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return "i=" + i + ", j=" + j + ", sim=" + String.format("%.3f", similarity);
        }


        /**
         * High value of similarity implies poor fit
         */
        @Override
        public int compareTo(Entry o) {
            if (o.similarity < this.similarity) return 1;
            else if (o.similarity > this.similarity) return -1;
            else return 0;
        }
    }

    /**
     * @param R Test Query
     * @param S Training Query
     * @return similarity
     */
    protected <T extends Number> double scoreX(List<T[]> R, List<T[]> S) {

        if (R.size() == S.size()) return couple(R, S);

        List<T[]> max = R.size() > S.size() ? R : S;

        List<T[]> min = R.size() < S.size() ? R : S;


        final List<Double> similarities = new ArrayList<>();


        Generator.combination(max)
                .simple(min.size())
                .stream()
                .forEach(sub -> {
                    similarities.add(couple(min, sub));
                });


        return way(similarities);
    }

    public static double[] array(Collection<Double> similarities) {
        double[] array = new double[similarities.size()];
        int i = 0;
        for (Double d : similarities)
            array[i++] = d;
        return array;
    }

    double way(List<Double> similarities) {
        switch (way) {

            case s:
                return similarities.stream().mapToDouble(Double::doubleValue).min().getAsDouble() + similarities.stream().mapToDouble(Double::doubleValue).max().getAsDouble();

            case m:
                return (similarities.stream().mapToDouble(Double::doubleValue).min().getAsDouble() + similarities.stream().mapToDouble(Double::doubleValue).max().getAsDouble()) / 2.0;

            case disjunctionMax: {
                Collections.sort(similarities);
                Collections.reverse(similarities);
                double sum = 0.0;
                for (int i = 1; i < similarities.size(); i++)
                    sum += similarities.get(i);

                return similarities.get(0) + 0.01 * sum;

            }

            case Mean:
                return similarities.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

            case x:
                return similarities.stream().mapToDouble(Double::doubleValue).max().getAsDouble();

            default:
                throw new AssertionError("unknown aggregation : " + way);
        }
    }


    /**
     * @param R Test Query
     * @param S Training Query
     * @return similarity
     */

    protected <T extends Number> double couple(List<T[]> R, List<T[]> S) {

        if (R.size() != S.size()) throw new RuntimeException("sizes are not equal!");

        List<Entry> list = entryList(R, S);

        Collections.sort(list);
        // System.out.println("initial = " + list);


        final double[] values = new double[R.size()];

        for (int i = 0; i < R.size(); i++) {
            Entry entry = list.remove(0);
            // System.out.println("removed = " + entry);
            values[i] = entry.similarity;
            remove(list, entry.i, entry.j);
            //  System.out.println(list);
        }

        if (!list.isEmpty()) throw new RuntimeException("list is not empty from couple similarity!");
        return aggregate(values);

    }

    double aggregate(double[] values) {
        switch (agg) {
            case Euclid:
                return Math.sqrt(StatUtils.sumSq(values)) / values.length;
            case Ari:
                return StatUtils.mean(values);

            case Geo: {

                for (int i = 0; i < values.length; i++)
                    if (values[i] == 0)
                        values[i] = 1.0;
                return StatUtils.geometricMean(values);

            }

            default:
                throw new AssertionError("unknown aggregation : " + agg);
        }
    }


    /**
     * @param R Test Query
     * @param S Training Query
     * @return similarity
     */
    private <T extends Number> double scoreOld(List<T[]> R, List<T[]> S) {

        List<Entry> list = entryList(R, S);

        Collections.sort(list);
        // System.out.println("initial = " + list);

        final int size = Math.min(R.size(), S.size());
        final double[] values = new double[size];

        for (int i = 0; i < size; i++) {
            Entry entry = list.remove(0);
            // System.out.println("removed = " + entry);
            values[i] = entry.similarity;
            remove(list, entry.i, entry.j);
            //  System.out.println(list);
        }

        return aggregate(values);

    }


    protected void remove(List<Entry> list, int i, int j) {
        list.removeIf(entry -> entry.i == i || entry.j == j);
    }

    @Override
    public String name() {
        return "C_" + aggregation() + "_" + chi.name();
    }
}
