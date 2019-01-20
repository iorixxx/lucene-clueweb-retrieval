package edu.anadolu.ltr;

import org.clueweb09.WarcRecord;

public interface IDocFeature {
    /**
     * Calculate a feature value from the warc Record
     *
     * @param warcRecord input warcRecord
     * @return the value of the feature
     */
    double calculate(WarcRecord warcRecord);

    String toString();
}
