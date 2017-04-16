package edu.anadolu.freq;

/**
 * Base class for the implementations of the term frequency normalization methods
 */
public abstract class TFNormalization {

    public final double avgFieldLength;

    TFNormalization(double avgFieldLength) {
        this.avgFieldLength = avgFieldLength;
    }

    public abstract double tfn(int tf, long docLength, double avgFieldLength);

    public double tfn(int tf, long docLength) {
        return tfn(tf, docLength, avgFieldLength);
    }

}
