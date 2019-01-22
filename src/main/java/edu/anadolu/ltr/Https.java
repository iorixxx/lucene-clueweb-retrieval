package edu.anadolu.ltr;

public class Https implements IDocFeature {

    IDocFeature f = (base) -> base.url.startsWith("https") ? 1 : 0;

    @Override
    public String toString() {
        return "Https";
    }

    @Override
    public double calculate(DocFeatureBase base) {
        return base.url.startsWith("https:") ? 1 : 0;
    }
}