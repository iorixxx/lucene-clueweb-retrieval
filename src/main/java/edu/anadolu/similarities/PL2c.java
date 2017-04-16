package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

import static edu.anadolu.similarities.PL2.LOG_2_OF_E;

/**
 * Parameterized PL2
 */
public final class PL2c extends ModelBase {

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double TF =
                tf * log2(1.0d + (c * averageDocumentLength) / docLength);
        double NORM = 1.0D / (TF + 1d);
        double f = (1.0D * termFrequency) / (1.0D * numberOfDocuments);
        return NORM
                * keyFrequency
                * (TF * log2(1.0D / f)
                + f * LOG_2_OF_E
                + 0.5d * log2(2 * Math.PI * TF)
                + TF * (log2(TF) - LOG_2_OF_E));
    }

    private final double c;

    /**
     * Constructs an instance of this class with the
     * specified value for the parameter c.
     *
     * @param c the term frequency normalisation parameter value.
     */
    public PL2c(double c) {
        this.c = c;
    }

    /**
     * Returns the name of the model.
     *
     * @return the name of the model
     */
    @Override
    public final String toString() {
        return "PL2c" + c;
    }
}
