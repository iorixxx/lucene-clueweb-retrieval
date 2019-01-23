package edu.anadolu.ltr;

import org.jsoup.nodes.Element;

public class Favicon implements IDocFeature {

    @Override
    public String toString() {
        return "Favicon";
    }

    @Override
    public double calculate(DocFeatureBase base) {

        Element icon = base.jDoc.head().select("link[rel=icon]").first();
        Element icon2 = base.jDoc.head().select("link[rel=shortcut icon]").first();
        Element meta = base.jDoc.head().select("meta[itemprop=image]").first();


        if (icon != null || meta != null || icon2 != null) {
            return 1;
        }

        return 0;
    }
}
