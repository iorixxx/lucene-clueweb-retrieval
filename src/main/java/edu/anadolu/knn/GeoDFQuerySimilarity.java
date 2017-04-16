package edu.anadolu.knn;

/**
 * First take geometric mean, and then normalize by DF
 */
public class GeoDFQuerySimilarity extends QuerySimilarityBase {

    public GeoDFQuerySimilarity(ChiBase chi, boolean zero) {
        super(chi, zero);
    }

    @Override
    public String name() {
        return "GDF";
    }

    @Override
    public double score(TFDAwareNeed R, TFDAwareNeed S) {
        if (zero)
            return chi.chiSquared(R.geoAndDFZero, S.geoAndDFZero);
        else
            return chi.chiSquared(R.geoAndDF, S.geoAndDF);
    }
}
