package edu.anadolu.ltr;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
public interface IQDFeature {

    default QDFeatureType type(){
        return QDFeatureType.SUM;
    }

    default QDFeatureFields field(){
        return QDFeatureFields.WHOLE;
    }

    /**
     * Calculate a feature value using the data provided by a DocFeatureBase instance
     *
     * @param base input docFeatureBase
     * @return the value of the feature
     */
    double calculate(QDFeatureBase base, String word, List<String> subParts) throws IOException;
}