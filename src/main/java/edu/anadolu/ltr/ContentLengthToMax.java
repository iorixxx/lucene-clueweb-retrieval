package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;

import java.io.IOException;
import java.util.List;


public class ContentLengthToMax implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        String text = base.jDoc.body().text();
        List<String> content = Analyzers.getAnalyzedTokens(text, Analyzers.analyzer(Tag.KStem));
        return content.size();
    }
}