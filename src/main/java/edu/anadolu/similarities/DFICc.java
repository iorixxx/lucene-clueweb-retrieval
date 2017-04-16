package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * Parameterized DFIC
 */
public class DFICc extends ModelBase {

    private final double c;

    /**
     * Constructs an instance of this class with the
     * specified value for the parameter c.
     *
     * @param c the term frequency normalisation parameter value.
     */
    public DFICc(double c) {
        this.c = c;
    }


    public double tfn(int tf, long docLength, double avgFieldLength) {
        return tf * log2(1.0d + (c * avgFieldLength / docLength));
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {

        double e_ij = (termFrequency * docLength) / (numberOfTokens);

        final double tfn = tfn((int) tf, docLength, averageDocumentLength);

        // Condition 1
        if (tfn <= e_ij) return 0D;

        double chiSquare = (Math.pow((tfn - e_ij), 2) / e_ij) + 1;

        return keyFrequency * log2(chiSquare);
    }

    @Override
    public String toString() {
        return "DFICc" + c;
    }
}
