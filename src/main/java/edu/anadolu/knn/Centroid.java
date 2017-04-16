package edu.anadolu.knn;

import java.util.List;

/**
 * Helper class for sorting queries according to chi-squared distance.
 */
public class Centroid implements Comparable<Centroid> {

    public final String model;
    final double similarity;

    public Centroid(String model, List<Double> similarities) {
        this.model = model;
        this.similarity = similarities.stream().mapToDouble(Double::doubleValue).average().orElse(-1.0);
    }

    @Override
    public String toString() {
        return model + "(" + similarity + ")";
    }

    /**
     * High value of similarity implies poor fit
     */
    @Override
    public int compareTo(Centroid o) {
        if (o.similarity < this.similarity) return 1;
        else if (o.similarity > this.similarity) return -1;
        else return 0;
    }

}