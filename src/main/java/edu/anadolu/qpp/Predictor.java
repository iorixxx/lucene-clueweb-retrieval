package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;

/**
 * Interface for query performance predictor
 */
public interface Predictor {

    double value(String word) throws IOException;

    double aggregated(InfoNeed need, Aggregate aggregate) throws IOException;
}
