package edu.anadolu.knn;

import java.util.List;

/**
 * interface responsible to select a model from k-nn neighbors
 */
public interface Voter {
    String vote(List<Pair> neighbors);
}
