package edu.anadolu.exp;

/**
 * Computes probability as p = (numDocs - cdf) / numDocs
 */
public class Prob2 extends Prob {

    public Prob2() {
        this(48693237);
    }

    public Prob2(long numDocs) {
        super(numDocs);
    }

    @Override
    public double prob(long cdf, long df) {
        return (numDocs - cdf) / (double) numDocs;
    }

    @Override
    public String toString() {
        return "P2";
    }
}
