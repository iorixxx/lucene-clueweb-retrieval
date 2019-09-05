package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class Keyword implements IDocFeature {


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        String keyword = MetaTag.enrich3(base.jDoc, "keywords");

        if (keyword != null)
            return 1;

        return 0;
    }
}
