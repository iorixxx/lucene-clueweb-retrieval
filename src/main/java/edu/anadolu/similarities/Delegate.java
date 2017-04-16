package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * DFI extension of term-weighting models
 * Returns zero when tf is under expected value
 */
public final class Delegate extends ModelBase {

    private final ModelBase delegate;

    public Delegate(ModelBase delegate) {
        this.delegate = delegate;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {

        final double e_ij = (termFrequency * docLength) / numberOfTokens;

        if (tf <= e_ij)
            return 0D;
        else
            return delegate.score(tf, docLength, averageDocumentLength, keyFrequency, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
    }

    @Override
    public String toString() {
        return delegate.toString() + "z";
    }
}
