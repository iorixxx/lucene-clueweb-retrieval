package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class Keyword implements IDocFeature {


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        if (base.keyword.size()>0)
            return 1;

        return 0;
    }
}
