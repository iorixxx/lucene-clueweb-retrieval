package edu.anadolu.knn;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.util.FastMath;

/**
 * Computes t test statistic for 1-sample t-test.
 */
public class TStats extends TTest {

    /**
     * Difference = sample1 - sample2 = our system - baseline
     *
     * @param sample2 baseline
     * @param sample1 our system
     * @return t statistics
     */
    public double tStats(final double[] sample2, final double[] sample1) {

        ensureDataConformance(sample2, sample1);

        //  z[i] = sample1[i] - sample2[i];
        // difference = our system - baseline

        double meanDifference = StatUtils.meanDifference(sample1, sample2);

        return t(meanDifference, 0,
                StatUtils.varianceDifference(sample1, sample2, meanDifference),
                sample1.length);

        // return FastMath.abs(t);

    }

    /**
     * TRisk implementation from
     * <p>
     * Hypothesis testing for the risk-sensitive evaluation of retrieval systems.
     * B. Taner Dincer, Craig Macdonald, and Iadh Ounis. 2014.
     * DOI: https://doi.org/10.1145/2600428.2609625
     *
     * @param base  baseline
     * @param run   run
     * @param alpha risk aversion parameter. Usually 1, 5, or 10.
     * @return TRisk score. Values less than -2 means significant risk, values greater than +2 means significant gain.
     */

    public static double tRisk(final double[] base, final double[] run, double alpha) {

        if (run.length != base.length)
            throw new RuntimeException("array lengths are not equal!");

        int n = base.length;
        double meanDifference = 0d;

        final double[] deltas = new double[base.length];

        for (int i = 0; i < n; i++) {

            double sdiff = run[i] - base[i];

            if (sdiff >= 0)
                deltas[i] = sdiff;
            else
                deltas[i] = (1d + alpha) * sdiff;

            meanDifference += deltas[i];

        }

        // [h,p,~,stats] = ttest(deltas, 0, 0.05, 'both');
        // return stats.tstats;

        meanDifference /= n;

        double sum1 = 0d;
        double sum2 = 0d;

        for (int i = 0; i < n; i++) {
            double diff = deltas[i];
            sum1 += (diff - meanDifference) * (diff - meanDifference);
            sum2 += diff - meanDifference;
        }
        double varianceDifference = (sum1 - (sum2 * sum2 / n)) / (n - 1);

        return meanDifference / Math.sqrt(varianceDifference / n);

    }

    /**
     * URisk implementation from
     * <p>
     * Reducing the risk of query expansion via robust constrained optimization.
     * Kevyn Collins-Thompson. 2009.
     * DOI: https://doi.org/10.1145/1645953.1646059
     *
     * @param base baseline
     * @param run  run
     * @return URisk score.
     */

    public static double URisk(double[] base, double[] run, double alpha) {

        if (run.length != base.length)
            throw new RuntimeException("array lengths are not equal!");

        final double[] win = new double[base.length];
        final double[] loss = new double[base.length];


        for (int i = 0; i < run.length; i++) {
            win[i] = Math.max(0, run[i] - base[i]);
            loss[i] = Math.max(0, base[i] - run[i]);
        }

        double reward = StatUtils.mean(win);
        double risk = StatUtils.mean(loss);

        return reward - (1 + alpha) * risk;


    }

    static double[] calculateDifferences(final double[] x, final double[] y) {

        final double[] z = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            z[i] = y[i] - x[i];
        }

        return z;
    }

    static double[] calculateAbsoluteDifferences(final double[] z)
            throws NullArgumentException, NoDataException {

        if (z == null) {
            throw new NullArgumentException();
        }

        if (z.length == 0) {
            throw new NoDataException();
        }

        final double[] zAbs = new double[z.length];

        for (int i = 0; i < z.length; ++i) {
            zAbs[i] = FastMath.abs(z[i]);
        }

        return zAbs;
    }

    private final NaturalRanking naturalRanking = new NaturalRanking(NaNStrategy.FIXED, TiesStrategy.AVERAGE);

    double z(final double[] x, final double[] y) {

        ensureDataConformance(x, y);

        // throws IllegalArgumentException if x and y are not correctly specified
        //  z[i] = y[i] - x[i];
        // difference = our system - baseline
        final double[] z = calculateDifferences(x, y);
        final double[] zAbs = calculateAbsoluteDifferences(z);

        final double[] ranks = naturalRanking.rank(zAbs);

        double Wplus = 0;

        for (int i = 0; i < z.length; ++i) {
            if (z[i] > 0) {
                Wplus += ranks[i];
            }
        }

        final int N = x.length;
        // final double Wminus = (((double) (N * (N + 1))) / 2.0) - Wplus;


        final double ES = (double) (N * (N + 1)) / 4.0;

        /* Same as (but saves computations):
         * final double VarW = ((double) (N * (N + 1) * (2*N + 1))) / 24;
         */
        final double VarS = ES * ((double) (2 * N + 1) / 6.0);

        // - 0.5 is a continuity correction
        return (Wplus - ES - 0.5) / FastMath.sqrt(VarS);


    }

    private void ensureDataConformance(final double[] x, final double[] y)
            throws NullArgumentException, NoDataException, DimensionMismatchException {

        if (x == null ||
                y == null) {
            throw new NullArgumentException();
        }
        if (x.length == 0 ||
                y.length == 0) {
            throw new NoDataException();
        }
        if (y.length != x.length) {
            throw new DimensionMismatchException(y.length, x.length);
        }
    }

}
