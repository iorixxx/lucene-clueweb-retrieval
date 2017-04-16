package edu.anadolu.knn;

/**
 * First normalize by DF, and then take arithmetic mean
 */
public class DFAverageQuerySimilarity extends QuerySimilarityBase {

    public DFAverageQuerySimilarity(ChiBase chi, boolean zero) {
        super(chi, zero);
    }

    @Override
    public double score(TFDAwareNeed R, TFDAwareNeed S) {
        if (zero)
            return chi.chiSquared(R.dfAndAverageZero, S.dfAndAverageZero);
        else
            return chi.chiSquared(R.dfAndAverage, S.dfAndAverage);
    }

    @Override
    public String name() {
        return "DFA";
    }
}
