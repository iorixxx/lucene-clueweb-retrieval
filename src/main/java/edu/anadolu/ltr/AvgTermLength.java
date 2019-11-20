package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;

import java.util.List;

public class AvgTermLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        List<String> terms = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(Tag.NoStem));
        return terms.stream().mapToInt(w -> w.length()).average().getAsDouble();
    }
}
