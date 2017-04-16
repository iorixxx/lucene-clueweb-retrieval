package edu.anadolu.knn;

/**
 * Relexation
 */
public enum Relax {
    Strong,  // query lengths must be equal to be
    Weak, // length differences can be maximum 1, OTQ isolated.
    Free // no rule at all
}
