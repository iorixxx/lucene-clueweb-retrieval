package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;

public class NoOfTitleTerms implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        return base.title.size();
    }
}
