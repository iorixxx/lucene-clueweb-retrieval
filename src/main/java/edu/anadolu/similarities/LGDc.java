package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * Parameterized LGD
 */
public final class LGDc extends ModelBase {

    private final double c;

    /**
     * Constructs an instance of this class with the
     * specified value for the parameter c.
     *
     * @param c the term frequency normalisation parameter value.
     */
    public LGDc(double c) {
        this.c = c;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double TF =
                tf * log2(1.0d + (c * averageDocumentLength) / docLength);
        double freq = (1.0D * documentFrequency) / (1.0D * numberOfDocuments);
        return
                keyFrequency
                        * log2((freq + TF) / freq);
    }

    /**
     * Returns the name of the model.
     *
     * @return the name of the model
     */
    @Override
    public String toString() {
        return "LGDc" + c;
    }

}
