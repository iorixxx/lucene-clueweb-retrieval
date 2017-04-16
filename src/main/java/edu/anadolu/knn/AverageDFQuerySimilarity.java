package edu.anadolu.knn;

/**
 * First take arithmetic mean, and then normalize by DF
 */
public class AverageDFQuerySimilarity extends QuerySimilarityBase {

    public AverageDFQuerySimilarity(ChiBase chi, boolean zero) {
        super(chi, zero);
    }

    @Override
    public double score(TFDAwareNeed R, TFDAwareNeed S) {
        if (zero)
            return chi.chiSquared(R.averageAndDFZero, S.averageAndDFZero);
        else
            return chi.chiSquared(R.averageAndDF, S.averageAndDF);
    }

    @Override
    public String name() {
        return "ADF";
    }

}
