package edu.anadolu.ltr;

import org.jsoup.select.Elements;


public class ImgToMax implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        Elements images = base.jDoc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
        return images.size();
    }
}

