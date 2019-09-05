package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class Copyright implements IDocFeature {


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        String copyright = MetaTag.enrich3(base.jDoc, "copyright");


        if (copyright != null) {
            return 1;
        }

        return 0;
    }
}
