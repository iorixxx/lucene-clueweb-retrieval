package edu.anadolu.similarities;

import edu.anadolu.freq.TFNormalization;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * Similarity based on square root of the term frequency normalization schemes
 *
 * @see edu.anadolu.freq.TFNormalization
 */
public final class SqrtTFN extends ModelBase {

    private final TFNormalization normalization;

    public SqrtTFN(TFNormalization normalization) {
        this.normalization = normalization;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return Math.sqrt(normalization.tfn((int) tf, docLength, averageDocumentLength));
    }

    @Override
    public String toString() {
        return "SqrtTFN" + normalization.toString();
    }
}
