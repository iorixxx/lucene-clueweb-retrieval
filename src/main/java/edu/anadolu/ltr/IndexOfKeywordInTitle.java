package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;


public class IndexOfKeywordInTitle implements IDocFeature {


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String title = base.jDoc.title();
            String keyword = MetaTag.enrich3(base.jDoc, "keywords");
            String[] keywords = keyword.split(",");
            int min = Integer.MAX_VALUE;
            for (String token : keywords) {
                if (title.contains(token)) {
                    if (title.indexOf(token) < min)
                        min = title.indexOf(token);
                }
            }
            if (min == Integer.MAX_VALUE) return 0;
            if (min == 0) return 1;
            return (double) 1 / min;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}

