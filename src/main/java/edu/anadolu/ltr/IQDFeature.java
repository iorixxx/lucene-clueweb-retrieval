package edu.anadolu.ltr;

import java.io.IOException;

@FunctionalInterface
public interface IQDFeature {
    /**
     * Calculate a feature value using the data provided by a DocFeatureBase instance
     *
     * @param base input docFeatureBase
     * @return the value of the feature
     */
    double calculate(QDFeatureBase base) throws IOException;
}