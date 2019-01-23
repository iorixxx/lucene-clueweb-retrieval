package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;

import java.io.IOException;
import java.util.List;

public class ContentLengthOver1800 implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        if(base.jDoc.body()==null) return 0;
        String text = base.jDoc.body().text();
        List<String> content = Analyzers.getAnalyzedTokens(text, Analyzers.analyzer(Tag.KStem));
        if (content.size() > 1800) return 1;
        return 0;
    }
}