package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;

import java.io.IOException;


public class SimKeywordDescription implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        String keyword = MetaTag.enrich3(base.jDoc, "keywords");
        String description = MetaTag.enrich3(base.jDoc, "description");
        return base.textSimilarity(keyword, description);
    }
}
