package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements a simple raw term frequency (TF) weighting model.
 */
public final class RawTF extends ModelBase {

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return tf;
    }

    @Override
    public String toString() {
        return "RawTF";
    }
}
