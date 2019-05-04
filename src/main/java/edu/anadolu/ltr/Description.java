package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class Description implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String description = MetaTag.enrich3(base.jDoc, "description");

        if (description != null)
            return 1;

        return 0;
    }
}