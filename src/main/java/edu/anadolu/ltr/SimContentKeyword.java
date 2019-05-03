package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;

import java.io.IOException;

public class SimContentKeyword implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        if(base.jDoc.body()==null) return 0;
        String content = base.getFirstWords(base.jDoc.body().text(),100);
        String keyword = MetaTag.enrich3(base.jDoc, "keywords");
        return base.textSimilarity(content, keyword);
    }
}
