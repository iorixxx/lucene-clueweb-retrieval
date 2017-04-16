package edu.anadolu.knn;

import org.clueweb09.InfoNeed;

/**
 * Helper class for sorting queries according to chi-squared distance.
 */
public class Pair implements Comparable<Pair> {

    public final InfoNeed infoNeed;
    final double similarity;

    public Pair(InfoNeed infoNeed, double similarity) {
        this.infoNeed = infoNeed;
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return infoNeed + "(" + similarity + ")";
    }

    /**
     * High value of similarity implies poor fit
     */
    @Override
    public int compareTo(Pair o) {
        if (o.similarity < this.similarity) return 1;
        else if (o.similarity > this.similarity) return -1;
        else return 0;
    }

    public String predictedModel;
}