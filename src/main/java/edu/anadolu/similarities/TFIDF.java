package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the TF_IDF weighting model.
 * tf is given by Robertson's tf and idf is given by the
 * standard Sparck Jones' idf [Sparck Jones, 1972].
 */
public final class TFIDF extends ModelBase {

    /**
     * The constant k_1.
     */
    private static final double k_1 = 1.2d;

    /**
     * The constant b.
     */
    private static final double b = 0.75d;

    protected float score(BasicStats stats, float tf, float docLength) {
        double Robertson_tf = k_1 * tf / (tf + k_1 * (1 - b + b * docLength / stats.getAvgFieldLength()));
        double idf = log2(((double) stats.getNumberOfDocuments() / (double) stats.getDocFreq()) + 1);
        double returnValue = Robertson_tf * idf;
        return (float) returnValue;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double Robertson_tf = k_1 * tf / (tf + k_1 * (1 - b + b * docLength / averageDocumentLength));
        double idf = log2(numberOfDocuments / documentFrequency + 1);
        return keyFrequency * Robertson_tf * idf;
    }

    @Override
    public String toString() {
        return "TFIDF";
    }
}
