package edu.anadolu.knn;

/**
 * Interface to calculate similarity of two queries
 */
public interface QuerySimilarity {

    double score(TFDAwareNeed R, TFDAwareNeed S);

    ChiBase chi();

    boolean zero();

    String name();
}
