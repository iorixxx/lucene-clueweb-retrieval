package edu.anadolu.similarities;

import edu.anadolu.freq.TFNormalization;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * Similarity based on logarithm of the term frequency normalization schemes
 *
 * @see edu.anadolu.freq.TFNormalization
 */
public final class LogTFN extends ModelBase {

    final int v;
    private final TFNormalization normalization;

    public LogTFN(TFNormalization normalization, int v) {
        this.normalization = normalization;
        assert v == 0 || v == 1 : "v can be zero or one";
        this.v = v;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return log2(v + normalization.tfn((int) tf, docLength, averageDocumentLength));
    }

    @Override
    public String toString() {
        return "LogTFN" + "v" + v + normalization.toString();
    }

}
