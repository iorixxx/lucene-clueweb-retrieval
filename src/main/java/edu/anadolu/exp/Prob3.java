package edu.anadolu.exp;

/**
 * Computes probability as p = (df - cdf) / numDocs
 */
public class Prob3 extends Prob {

    public Prob3() {
        this(48693237);
    }

    public Prob3(long numDocs) {
        super(numDocs);
    }

    @Override
    public double prob(long cdf, long df) {
        return (df - cdf) / (double) numDocs;
    }

    @Override
    public String toString() {
        return "P3";
    }
}