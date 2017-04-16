package edu.anadolu.exp;

/**
 * Computes probability as p = (df - cdf) / df
 */
public class Prob1 extends Prob {

    public Prob1() {
        this(48693237);
    }

    public Prob1(long numDocs) {
        super(numDocs);
    }

    @Override
    public double prob(long cdf, long df) {
        return (df - cdf) / (double) df;
    }

    @Override
    public String toString() {
        return "P1";
    }
}
