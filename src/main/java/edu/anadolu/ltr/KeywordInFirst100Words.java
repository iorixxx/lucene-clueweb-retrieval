package edu.anadolu.ltr;


import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.field.MetaTag;

import java.util.List;

public class KeywordInFirst100Words implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String text = base.jDoc.body().text();
            String keyword = MetaTag.enrich3(base.jDoc, "keywords");
            List<String> content = Analyzers.getAnalyzedTokens(text, Analyzers.analyzer(Tag.NoStem));
            String first100words = "";
            for (int i = 0; i < 100; i++) {
                first100words += content.get(i);
            }
            for (String token : Analyzers.getAnalyzedTokens(keyword, Analyzers.analyzer(Tag.NoStem))) {
                if (first100words.contains(token))
                    return 1;
            }
            return 0;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}
