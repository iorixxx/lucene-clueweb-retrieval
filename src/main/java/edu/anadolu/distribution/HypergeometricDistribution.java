package edu.anadolu.distribution;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.FastMath;

/**
 * Long type compatible "copy-paste and find-replace" version of the hypergeometric distribution from commons-math3 version 3.4.1.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Hypergeometric_distribution">Hypergeometric distribution (Wikipedia)</a>
 * @see <a href="http://mathworld.wolfram.com/HypergeometricDistribution.html">Hypergeometric distribution (MathWorld)</a>
 */
public class HypergeometricDistribution {
    /**
     * The number of successes in the population.
     */
    private final long numberOfSuccesses;
    /**
     * The population size.
     */
    private final long populationSize;
    /**
     * The sample size.
     */
    private final long sampleSize;
    /**
     * Cached numerical variance
     */
    private double numericalVariance = Double.NaN;
    /**
     * Whether or not the numerical variance has been calculated
     */
    private boolean numericalVarianceIsCalculated = false;

    /**
     * Construct a new hypergeometric distribution with the specified population
     * size, number of successes in the population, and sample size.
     * <p>
     * <b>Note:</b> this constructor will implicitly create an instance of
     * {@link Well19937c} as random generator to be used for sampling only (see
     * {@link AbstractIntegerDistribution#sample()} and {@link AbstractIntegerDistribution#sample(int)}). In case no sampling is
     * needed for the created distribution, it is advised to pass {@code null}
     * as random generator via the appropriate constructors to avoid the
     * additional initialisation overhead.
     *
     * @param populationSize    Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize        Sample size.
     * @throws NotPositiveException         if {@code numberOfSuccesses < 0}.
     * @throws NotStrictlyPositiveException if {@code populationSize <= 0}.
     * @throws NumberIsTooLargeException    if {@code numberOfSuccesses > populationSize},
     *                                      or {@code sampleSize > populationSize}.
     */
    public HypergeometricDistribution(long populationSize, long numberOfSuccesses, long sampleSize)
            throws NotPositiveException, NotStrictlyPositiveException, NumberIsTooLargeException {
        this(null, populationSize, numberOfSuccesses, sampleSize);
    }

    /**
     * Creates a new hypergeometric distribution.
     *
     * @param rng               Random number generator.
     * @param populationSize    Population size.
     * @param numberOfSuccesses Number of successes in the population.
     * @param sampleSize        Sample size.
     * @throws NotPositiveException         if {@code numberOfSuccesses < 0}.
     * @throws NotStrictlyPositiveException if {@code populationSize <= 0}.
     * @throws NumberIsTooLargeException    if {@code numberOfSuccesses > populationSize},
     *                                      or {@code sampleSize > populationSize}.
     * @since 3.1
     */
    public HypergeometricDistribution(RandomGenerator rng,
                                      long populationSize,
                                      long numberOfSuccesses,
                                      long sampleSize)
            throws NotPositiveException, NotStrictlyPositiveException, NumberIsTooLargeException {


        if (populationSize <= 0) {
            throw new NotStrictlyPositiveException(LocalizedFormats.POPULATION_SIZE,
                    populationSize);
        }
        if (numberOfSuccesses < 0) {
            throw new NotPositiveException(LocalizedFormats.NUMBER_OF_SUCCESSES,
                    numberOfSuccesses);
        }
        if (sampleSize < 0) {
            throw new NotPositiveException(LocalizedFormats.NUMBER_OF_SAMPLES,
                    sampleSize);
        }

        if (numberOfSuccesses > populationSize) {
            throw new NumberIsTooLargeException(LocalizedFormats.NUMBER_OF_SUCCESS_LARGER_THAN_POPULATION_SIZE,
                    numberOfSuccesses, populationSize, true);
        }
        if (sampleSize > populationSize) {
            throw new NumberIsTooLargeException(LocalizedFormats.SAMPLE_SIZE_LARGER_THAN_POPULATION_SIZE,
                    sampleSize, populationSize, true);
        }

        this.numberOfSuccesses = numberOfSuccesses;
        this.populationSize = populationSize;
        this.sampleSize = sampleSize;
    }

    /**
     * {@inheritDoc}
     */
    public double cumulativeProbability(int x) {
        double ret;

        long[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x < domain[0]) {
            ret = 0.0;
        } else if (x >= domain[1]) {
            ret = 1.0;
        } else {
            ret = innerCumulativeProbability(domain[0], x, 1L);
        }

        return ret;
    }

    /**
     * Return the domain for the given hypergeometric distribution parameters.
     *
     * @param n Population size.
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return a two element array containing the lower and upper bounds of the
     * hypergeometric distribution.
     */
    private long[] getDomain(long n, long m, long k) {
        return new long[]{getLowerDomain(n, m, k), getUpperDomain(m, k)};
    }

    /**
     * Return the lowest domain value for the given hypergeometric distribution
     * parameters.
     *
     * @param n Population size.
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return the lowest domain value of the hypergeometric distribution.
     */
    private long getLowerDomain(long n, long m, long k) {
        return FastMath.max(0, m - (n - k));
    }

    /**
     * Access the number of successes.
     *
     * @return the number of successes.
     */
    public long getNumberOfSuccesses() {
        return numberOfSuccesses;
    }

    /**
     * Access the population size.
     *
     * @return the population size.
     */
    public long getPopulationSize() {
        return populationSize;
    }

    /**
     * Access the sample size.
     *
     * @return the sample size.
     */
    public long getSampleSize() {
        return sampleSize;
    }

    /**
     * Return the highest domain value for the given hypergeometric distribution
     * parameters.
     *
     * @param m Number of successes in the population.
     * @param k Sample size.
     * @return the highest domain value of the hypergeometric distribution.
     */
    private long getUpperDomain(long m, long k) {
        return FastMath.min(k, m);
    }

    /**
     * {@inheritDoc}
     */
    public double probability(long x) {
        final double logProbability = logProbability(x);
        return logProbability == Double.NEGATIVE_INFINITY ? 0 : FastMath.exp(logProbability);
    }

    /**
     * {@inheritDoc}
     */

    public double logProbability(long x) {
        double ret;

        long[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x < domain[0] || x > domain[1]) {
            ret = Double.NEGATIVE_INFINITY;
        } else {
            double p = (double) sampleSize / (double) populationSize;
            double q = (double) (populationSize - sampleSize) / (double) populationSize;
            double p1 = SaddlePointExpansion.logBinomialProbability(x,
                    numberOfSuccesses, p, q);
            double p2 =
                    SaddlePointExpansion.logBinomialProbability(sampleSize - x,
                            populationSize - numberOfSuccesses, p, q);
            double p3 =
                    SaddlePointExpansion.logBinomialProbability(sampleSize, populationSize, p, q);
            ret = p1 + p2 - p3;
        }

        return ret;
    }

    /**
     * For this distribution, {@code X}, this method returns {@code P(X >= x)}.
     *
     * @param x Value at which the CDF is evaluated.
     * @return the upper tail CDF for this distribution.
     * @since 1.1
     */
    public double upperCumulativeProbability(int x) {
        double ret;

        final long[] domain = getDomain(populationSize, numberOfSuccesses, sampleSize);
        if (x <= domain[0]) {
            ret = 1.0;
        } else if (x > domain[1]) {
            ret = 0.0;
        } else {
            ret = innerCumulativeProbability(domain[1], x, -1);
        }

        return ret;
    }

    /**
     * For this distribution, {@code X}, this method returns
     * {@code P(x0 <= X <= x1)}.
     * This probability is computed by summing the point probabilities for the
     * values {@code x0, x0 + 1, x0 + 2, ..., x1}, in the order directed by
     * {@code dx}.
     *
     * @param x0 Inclusive lower bound.
     * @param x1 Inclusive upper bound.
     * @param dx Direction of summation (1 indicates summing from x0 to x1, and
     *           0 indicates summing from x1 to x0).
     * @return {@code P(x0 <= X <= x1)}.
     */
    private double innerCumulativeProbability(long x0, long x1, long dx) {
        double ret = probability(x0);
        while (x0 != x1) {
            x0 += dx;
            ret += probability(x0);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * <p>
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the mean is {@code n * m / N}.
     */
    public double getNumericalMean() {
        return getSampleSize() * (getNumberOfSuccesses() / (double) getPopulationSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the variance is
     * {@code [n * m * (N - n) * (N - m)] / [N^2 * (N - 1)]}.
     */
    public double getNumericalVariance() {
        if (!numericalVarianceIsCalculated) {
            numericalVariance = calculateNumericalVariance();
            numericalVarianceIsCalculated = true;
        }
        return numericalVariance;
    }

    /**
     * Used by {@link #getNumericalVariance()}.
     *
     * @return the variance of this distribution
     */
    protected double calculateNumericalVariance() {
        final double N = getPopulationSize();
        final double m = getNumberOfSuccesses();
        final double n = getSampleSize();
        return (n * m * (N - n) * (N - m)) / (N * N * (N - 1));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For population size {@code N}, number of successes {@code m}, and sample
     * size {@code n}, the lower bound of the support is
     * {@code max(0, n + m - N)}.
     *
     * @return lower bound of the support
     */
    public long getSupportLowerBound() {
        return FastMath.max(0,
                getSampleSize() + getNumberOfSuccesses() - getPopulationSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * For number of successes {@code m} and sample size {@code n}, the upper
     * bound of the support is {@code min(m, n)}.
     *
     * @return upper bound of the support
     */
    public long getSupportUpperBound() {
        return FastMath.min(getNumberOfSuccesses(), getSampleSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The support of this distribution is connected.
     *
     * @return {@code true}
     */
    public boolean isSupportConnected() {
        return true;
    }

    static void displayPDF(HypergeometricDistribution geometric, long docLength) {
        for (int i = 0; i <= docLength; i++) {
            double probability = geometric.probability(i);
            String prob = String.format("%.6f", probability);
            System.out.println(i + "\t" + prob);
            if ("0.000000".equals(prob)) break;
        }
    }

    public static void main(String[] args) {

        // N = is the population size = populationSize = 37407079119
        // K = is the number of success states in the population = numberOfSuccesses = TF
        // n = is the number of draws = sampleSize = document length

        long populationSize = 37407079119L;

        long docLength = 450;
        long texas_TF = 5322222;
        long who_TF = 41542856;
        long be_TF = 120255922;

        System.out.println("pdf of the term \"texas\" drawn from ClueWeb09B");
        displayPDF(new HypergeometricDistribution(populationSize, texas_TF, docLength), docLength);

        System.out.println("pdf of the term \"who\" drawn from ClueWeb09B");
        displayPDF(new HypergeometricDistribution(populationSize, who_TF, docLength), docLength);

        System.out.println("pdf of the term \"be\" drawn from ClueWeb09B");
        displayPDF(new HypergeometricDistribution(populationSize, be_TF, docLength), docLength);


    }
}
