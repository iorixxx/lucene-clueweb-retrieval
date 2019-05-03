package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.jsoup.select.Elements;

import java.io.IOException;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimContentH implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        if(base.jDoc.body()==null) return 0;
        String content = base.getFirstWords(base.jDoc.body().text(),100);
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
        return base.textSimilarity(content, h);
    }
}
