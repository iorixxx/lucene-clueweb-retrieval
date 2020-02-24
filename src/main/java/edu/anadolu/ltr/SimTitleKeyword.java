package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;

import java.io.IOException;


public class SimTitleKeyword implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        return base.textSimilarity(base.title, base.keyword);
    }
}
