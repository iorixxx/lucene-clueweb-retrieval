package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * Similarity based on sums of relative frequencies of query terms.
 */
public final class MetaTerm extends ModelBase {

    protected float score(BasicStats stats, float freq, float docLen) {
        return freq / docLen;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return tf / docLength;
    }

    @Override
    public String toString() {
        return "MetaTerm";
    }
}
