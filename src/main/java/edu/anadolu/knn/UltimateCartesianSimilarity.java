package edu.anadolu.knn;

import org.apache.commons.math3.stat.StatUtils;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ultimate cartesian term query similarity
 */
public class UltimateCartesianSimilarity extends CartesianQueryTermSimilarity {

    public UltimateCartesianSimilarity(ChiBase chi, boolean zero, Aggregation agg, Way way) {
        super(chi, zero, agg, way);
    }

    @Override
    public String name() {
        return "U" + aggregation();
    }

    @Override
    protected <T extends Number> double scoreX(List<T[]> R, List<T[]> S) {


        final List<T[]> max = R.size() > S.size() ? R : S;

        final List<T[]> min = R.size() < S.size() ? R : S;

        if (R.size() == S.size()) {
            return couple(R, S);
        }

        List<Double> similarities = new ArrayList<>();

        Generator.combination(max)
                .simple(min.size())
                .stream()
                .forEach(sub -> {
                    similarities.add(couple(min, sub));
                });

        boolean oneOfThemIsOTQ = R.size() == 1 || S.size() == 1;
        boolean useAll = Math.abs(R.size() - S.size()) > 1;


        return way(similarities, Math.abs(R.size() - S.size()) + 1);
    }

    double way(List<Double> similarities, int howMany) {

        Collections.sort(similarities);

        List<Double> subList = similarities.subList(0, howMany);

        switch (way) {


            case s: {

                double[] d = array(similarities);
                return StatUtils.min(d) + StatUtils.max(d);
            }
            case m: {

                double[] d = array(similarities);
                return (StatUtils.min(d) + StatUtils.max(d)) / 2.0;
            }


            case Mean:
                return StatUtils.mean(array(subList));

            default:
                throw new AssertionError("unknown way : " + way);
        }
    }

    public static void main(String[] args) {

        List<Double> sims = new ArrayList<>(10);
        sims.add(10.0);
        sims.add(5.6);
        sims.add(1.2);

        Collections.sort(sims);
        System.out.println(sims);

        List<Double> similarities = sims.subList(0, 2);

        System.out.println(similarities);
    }


}
