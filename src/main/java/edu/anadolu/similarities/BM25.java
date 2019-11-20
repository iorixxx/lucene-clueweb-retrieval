package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the Okapi BM25 weighting model. The
 * default parameters used are:<br>
 * k_1 = 1.2d<br>
 * k_3 = 8d<br>
 * b = 0.75d<br> The b parameter can be altered by using the setParameter method.
 */
public final class BM25 extends ModelBase {

    /**
     * The constant k_1.
     */
    private static final double k_1 = 1.2d;

    /**
     * The constant k_3.
     */
    private static final double k_3 = 8d;

    /**
     * The parameter b.
     */
    private double b;

    /**
     * A default constructor.
     */
    public BM25() {
        super();
        b = 0.75d;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double K = k_1 * ((1 - b) + b * docLength / averageDocumentLength) + tf;
        return (tf * (k_3 + 1d) * keyFrequency / ((k_3 + keyFrequency) * K))
                * log2((numberOfDocuments - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
    }

    protected float score(BasicStats stats, float tf, float docLength) {

        double K = k_1 * ((1 - b) + b * docLength / stats.getAvgFieldLength()) + tf;

        double keyFrequency = 1.0d;

        double returnValue = (tf * (k_3 + 1d) * keyFrequency / ((k_3 + keyFrequency) * K))
                * log2((stats.getNumberOfDocuments() - stats.getDocFreq() + 0.5d) / (stats.getDocFreq() + 0.5d));

        return (float) returnValue;

    }

    @Override
    public String toString() {
        return "BM25";
    }

}
