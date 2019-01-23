package edu.anadolu.ltr;

import org.jsoup.select.Elements;

import java.io.IOException;

import static edu.anadolu.field.MetaTag.notEmpty;


public class SimTitleH implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        Elements hTags = base.jDoc.select("h1, h2, h3, h4, h5, h6");

        //TODO think how to handle
        if(hTags.size()==0) return 0;
        
        StringBuilder builder = new StringBuilder();

        hTags.stream()
                .map(e -> e.text())
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        hTags.empty();
        String h = builder.toString().trim();
        String title = base.jDoc.title();
        return base.textSimilarity(title, h);
    }
}

