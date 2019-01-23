package edu.anadolu.ltr;

import org.jsoup.select.Elements;


public class MetaTagToMax implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        Elements elements = base.jDoc.select("meta");
        return (double) elements.size();
    }
}