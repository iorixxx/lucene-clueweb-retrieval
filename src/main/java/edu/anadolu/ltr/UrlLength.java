package edu.anadolu.ltr;

import org.apache.solr.common.StringUtils;

public class UrlLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        //TODO think how to handle
        if (StringUtils.isEmpty(base.url))
            return 0;
        return (double) base.url.length();
    }
}
