package edu.anadolu.knn;

import org.clueweb09.InfoNeed;

/**
 * Encapsulates information regarding predictions made by selective term-weighting framework
 */
public class Prediction {

    public final InfoNeed testQuery;
    public final String predictedModel;
    public final double predictedScore;

    public Prediction(InfoNeed testQuery, String predictedModel, double predictedScore) {
        this.predictedModel = predictedModel;
        this.testQuery = testQuery;
        this.predictedScore = predictedScore;
    }
}
