package edu.anadolu.ltr;

import org.apache.solr.common.StringUtils;
import org.jsoup.select.Elements;

import java.util.function.Predicate;


public class HdensityToMax implements IDocFeature {

    private final Predicate<String> notEmpty = (String s) -> s != null && !s.isEmpty();


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        if(base.jDoc.body()==null) return 0;

        Elements hTags = base.jDoc.select("h1, h2, h3, h4, h5, h6");

        StringBuilder builder = new StringBuilder();

        hTags.stream()
                .map(e -> e.text())
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        hTags.empty();
        return (double) (builder.toString().trim().length()) / base.jDoc.body().text().length();
    }
}