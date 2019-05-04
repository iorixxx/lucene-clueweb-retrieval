package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class Viewport implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String viewport = MetaTag.enrich3(base.jDoc, "viewport");
        if (viewport != null) {
            return 1;
        }

        return 0;
    }
}
