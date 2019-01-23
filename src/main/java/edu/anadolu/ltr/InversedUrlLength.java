package edu.anadolu.ltr;

import org.apache.solr.common.StringUtils;

public class InversedUrlLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        //TODO think how to handle
        if (StringUtils.isEmpty(base.url))
            return 0;
        return (double) 1 / base.url.length();
    }
}
