package edu.anadolu.freq;

import static org.apache.lucene.search.similarities.ModelBase.log2;

/**
 * Creates NormalizationH2 with respect to the document length.
 *
 * @see org.apache.lucene.search.similarities.NormalizationH2
 */
public final class L2 extends TFNormalization {

    public L2() {
        super(37407079119L / (double) 48735852);
    }

    public L2(double avgFieldLength) {
        super(avgFieldLength);
    }

    @Override
    public double tfn(int tf, long docLength, double avgFieldLength) {
        return tf * log2(1.0d + (avgFieldLength / docLength));
    }

    @Override
    public String toString() {
        return "L2";
    }
}

