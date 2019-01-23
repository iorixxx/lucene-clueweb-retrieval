package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.jsoup.select.Elements;

import java.io.IOException;

import static edu.anadolu.field.MetaTag.notEmpty;


public class SimDescriptionH implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        Elements hTags = base.jDoc.select("h1, h2, h3, h4, h5, h6");

        StringBuilder builder = new StringBuilder();

        hTags.stream()
                .map(e -> e.text())
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        hTags.empty();
        String h = builder.toString().trim();
        String description = MetaTag.enrich3(base.jDoc, "description");
        return base.textSimilarity(description, h);
    }
}

