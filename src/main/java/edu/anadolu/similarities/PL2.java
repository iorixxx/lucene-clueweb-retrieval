package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the PL2 weighting model.
 */
public final class PL2 extends ModelBase {

    /**
     * Precomputed for efficiency reasons.
     */
    public static final double LOG_2_OF_E = Math.log(Math.E);

    protected float score(BasicStats stats, float tf, float docLength) {

        double TF = tf * log2(1.0d + (stats.getAvgFieldLength()) / docLength);

        double NORM = 1.0D / (TF + 1d);

        double f = (1.0D * stats.getTotalTermFreq()) / (1.0D * stats.getNumberOfDocuments());

        double returnValue = NORM

                * (TF * log2(1.0D / f)
                + f * LOG_2_OF_E
                + 0.5d * log2(2 * Math.PI * TF)
                + TF * (log2(TF) - LOG_2_OF_E));

        return (float) returnValue;

    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double TF =
                tf * log2(1.0d + (averageDocumentLength) / docLength);
        double NORM = 1.0D / (TF + 1d);
        double f = (1.0D * termFrequency) / (1.0D * numberOfDocuments);
        return NORM
                * keyFrequency
                * (TF * log2(1.0D / f)
                + f * LOG_2_OF_E
                + 0.5d * log2(2 * Math.PI * TF)
                + TF * (log2(TF) - LOG_2_OF_E));
    }

    @Override
    public String toString() {
        return "PL2";
    }
}
