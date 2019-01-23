package edu.anadolu.ltr;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class SocialMediaShare implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        Elements links = base.jDoc.select("a");
        for (Element element : links) {
            String href = element.attr("href");
            if (href.contains("facebook") && href.contains("share")) return 1;
            if (href.contains("plus.google") && href.contains("share")) return 1;
            if (href.contains("linkedin") && href.contains("share")) return 1;
            if (href.contains("digg") && href.contains("submit")) return 1;
            if (href.contains("reddit") && href.contains("submit")) return 1;
        }
        return 0;
    }
}
