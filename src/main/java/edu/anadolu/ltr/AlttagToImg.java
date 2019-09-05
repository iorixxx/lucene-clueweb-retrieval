package edu.anadolu.ltr;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class AlttagToImg implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        int count = 0;
        Elements images = base.jDoc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");

        //TODO think how to handle pages with no images
        if (images.size() == 0) return 0;


        for (Element image : images) {
            if (image.attr("alt") == null) continue;
            if (image.attr("alt").equals("")) continue;
            count++;
        }
        return (double) count / images.size();
    }
}
