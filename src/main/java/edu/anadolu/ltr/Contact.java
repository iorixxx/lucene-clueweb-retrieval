package edu.anadolu.ltr;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Contact implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        Elements links = base.jDoc.select("a");
        for (Element element : links) {
            String href = element.attr("href");
            if (href.contains("mailto:")) return 1;
        }
        return 0;
    }
}
