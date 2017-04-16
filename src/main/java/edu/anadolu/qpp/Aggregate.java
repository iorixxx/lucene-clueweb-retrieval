package edu.anadolu.qpp;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Specificity predictors are calculated for each query term.
 * Values are aggregated by implementations of this interface.
 */
public interface Aggregate {

    double aggregate(double[] values);

    /**
     * A value of "0.0" makes the query a pure "disjunction max query" -- only the maximum scoring sub query contributes to the final score.
     * A value of "1.0" makes the query a pure "disjunction sum query" where it doesn't matter what the maximum scoring sub query is, the final score is the sum of the sub scores.
     * Typically a low value (ie: 0.1) is useful.
     * <p>
     * a document matching a single query term over many fields could get unreasonably higher score than another document which matches all the query terms in a few fields.
     */
    class DisMax implements Aggregate {

        final double tie;

        public DisMax(double tie) {
            this.tie = tie;
        }

        @Override
        public double aggregate(double[] values) {
            final double max = StatUtils.max(values);
            double sum = 0.0;
            for (double d : values)
                if (max != d)
                    sum += d;
            return max + tie * sum;
        }

        @Override
        public String toString() {
            return "dismax" + tie;
        }
    }

    class DisMin implements Aggregate {

        final double tie;

        public DisMin(double tie) {
            this.tie = tie;
        }

        @Override
        public double aggregate(double[] values) {
            final double min = StatUtils.min(values);
            double sum = 0.0;
            for (double d : values)
                if (min != d)
                    sum += d;
            return min + tie * sum;
        }

        @Override
        public String toString() {
            return "dismin" + tie;
        }
    }

    class Gamma1 implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.min(values) / StatUtils.max(values);
        }

        @Override
        public String toString() {
            return "gamma1";
        }

    }

    class Gamma2 implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.max(values) / StatUtils.min(values);
        }

        @Override
        public String toString() {
            return "gamma2";
        }

    }

    class Minimum implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.min(values);
        }

        @Override
        public String toString() {
            return "min";
        }
    }

    class Maximum implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.max(values);
        }

        @Override
        public String toString() {
            return "max";
        }
    }

    class Average implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.mean(values);
        }

        @Override
        public String toString() {
            return "avg";
        }
    }


    class Variance implements Aggregate {

        @Override
        public double aggregate(double[] values) {
            return StatUtils.variance(values);
        }

        @Override
        public String toString() {
            return "var";
        }
    }
}

