package edu.anadolu.similarities;

/**
 * Variant of DPH that returns 0.0 instead of negative values
 */
public final class DPHp extends DPH {

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double f = super.score(tf, docLength, averageDocumentLength, keyFrequency, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
        return f < 0 ? 0 : f;
    }

    @Override
    public String toString() {
        return "DPHp";
    }
}
