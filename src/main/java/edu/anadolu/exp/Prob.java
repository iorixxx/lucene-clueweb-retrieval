package edu.anadolu.exp;

/**
 * Abstract class that calculates probability of Prob(X >= tfn)
 */
public abstract class Prob {

    protected final long numDocs;

    public Prob(long numDocs) {
        this.numDocs = numDocs;
    }

    public abstract double prob(long cdf, long df);
}
