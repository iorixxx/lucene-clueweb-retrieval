package edu.anadolu.freq;

/**
 * No Normalization
 *
 * @see org.apache.lucene.search.similarities.Normalization.NoNormalization
 */
public final class L0 extends TFNormalization {

    public L0() {
        super(37407079119L / (double) 48735852);
    }

    @Override
    public double tfn(int tf, long docLength, double avgFieldLength) {
        return (double) tf;
    }

    @Override
    public String toString() {
        return "L0";
    }
}
