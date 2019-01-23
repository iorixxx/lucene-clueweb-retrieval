package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;

public class Robots implements IDocFeature {


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        String robots = MetaTag.enrich3(base.jDoc, "robots");

        if (robots != null) {
            return 1;
        }

        return 0;
    }
}