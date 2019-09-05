package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.jsoup.select.Elements;

import java.io.IOException;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimKeywordH implements IDocFeature {

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
        String keyword = MetaTag.enrich3(base.jDoc, "keywords");
        return base.textSimilarity(keyword, h);
    }
}

