package edu.anadolu.knn;

import org.clueweb09.InfoNeed;

import java.util.List;


public class Fold {

    List<InfoNeed> trainingQueries;

    List<InfoNeed> testQueries;

    public Fold(List<InfoNeed> trainingQueries, List<InfoNeed> testQueries) {
        this.testQueries = testQueries;
        this.trainingQueries = trainingQueries;
    }
}
