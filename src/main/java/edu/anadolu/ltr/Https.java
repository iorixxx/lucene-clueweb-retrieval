package edu.anadolu.ltr;


import org.apache.solr.common.StringUtils;

public class Https implements IDocFeature {

//    IDocFeature f = (base) -> base.url.startsWith("https") ? 1 : 0;

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        if(StringUtils.isEmpty(base.url)) {
            //TODO think how to handle
            return 0;
        }
        return base.url.startsWith("https:") ? 1 : 0;
    }
}