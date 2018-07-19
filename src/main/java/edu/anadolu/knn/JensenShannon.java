package edu.anadolu.knn;

import static org.apache.lucene.search.similarities.ModelBase.log2;

/**
 * The Jensen–Shannon divergence is a popular method of measuring the similarity between two probability distributions.
 */
public class JensenShannon extends ChiBase {


    public JensenShannon(boolean divide) {
        super(divide, false);
    }

    @Override
    public <T extends Number> double chiCDF(T[] R, T[] S) {
        throw new UnsupportedOperationException("Jensen-Shannon divergence works on pdf only!");
    }

    @Override
    String name() {
        return "JS";
    }

    @Override
    public <T extends Number> double chiPDF(T[] R, T[] S) {
        return js(R, S) + js(S, R);
    }

    /**
     * The Jensen–Shannon divergence (JSD) is a symmetrized and smoothed version of the Kullback–Leibler divergence
     */
    private <T extends Number> double js(T[] R, T[] S) {

        double js = 0.0;

        int counter = 0;

        for (int i = 0; i < R.length; i++) {

            if (R[i].doubleValue() == 0 && S[i].doubleValue() == 0) continue;
            if (R[i].doubleValue() == 0) continue;
            counter++;
            js += R[i].doubleValue() * log2(R[i].doubleValue() / (0.5 * R[i].doubleValue() + 0.5 * S[i].doubleValue()));
        }

        if (divide)
            return js / (double) counter;
        else
            return js;
    }

    public String toString() {
        return ",JS"; //+ super.toString();
    }
}
