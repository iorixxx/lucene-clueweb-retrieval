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
