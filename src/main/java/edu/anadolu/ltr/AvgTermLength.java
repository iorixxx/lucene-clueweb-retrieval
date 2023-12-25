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
        return base.mapTf.keySet().stream().mapToInt(w -> w.length()).average().getAsDouble();
    }
}
