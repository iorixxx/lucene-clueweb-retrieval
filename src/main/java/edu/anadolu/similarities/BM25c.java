package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * Parameterized BM25
 */
public class BM25c extends ModelBase {

    private final double k_1;

    /**
     * The constant k_3.
     */
    private static final double k_3 = 8d;

    /**
     * The parameter b.
     */
    private final double b;

    public BM25c(double k_1, double b) {
        this.k_1 = k_1;
        this.b = b;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double K = k_1 * ((1 - b) + b * docLength / averageDocumentLength) + tf;
        return (tf * (k_3 + 1d) * keyFrequency / ((k_3 + keyFrequency) * K))
                * log2((numberOfDocuments - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
    }

    @Override
    public String toString() {
        return "BM25k" + k_1 + "b" + b;
    }
}
