package edu.anadolu.knn;

/**
 * First normalize by DF, and then take geometric mean
 */
public class DFGeoQuerySimilarity extends QuerySimilarityBase {

    public DFGeoQuerySimilarity(ChiBase chi, boolean zero) {
        super(chi, zero);
    }

    @Override
    public double score(TFDAwareNeed R, TFDAwareNeed S) {
        if (zero)
            return chi.chiSquared(R.dfAndGeoZero, S.dfAndGeoZero);
        else
            return chi.chiSquared(R.dfAndGeo, S.dfAndGeo);
    }

    @Override
    public String name() {
        return "DFG";
    }
}
