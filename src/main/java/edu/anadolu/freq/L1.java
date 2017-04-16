package edu.anadolu.freq;

/**
 * Creates NormalizationH1 with respect to the document length.
 *
 * @see org.apache.lucene.search.similarities.NormalizationH1
 */
public final class L1 extends TFNormalization {

    public L1() {
        super(37407079119L / (double) 48735852);
    }

    public L1(double avgFieldLength) {
        super(avgFieldLength);
    }

    @Override
    public double tfn(int tf, long docLength, double avgFieldLength) {
        return tf * avgFieldLength / docLength;
    }

    @Override
    public String toString() {
        return "L1";
    }
}
