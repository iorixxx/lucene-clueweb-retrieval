package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;

import java.io.IOException;


public class SimTitleDescription implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        String title = base.jDoc.title();
        String description = MetaTag.enrich3(base.jDoc, "description");
        return base.textSimilarity(title, description);
    }
}
