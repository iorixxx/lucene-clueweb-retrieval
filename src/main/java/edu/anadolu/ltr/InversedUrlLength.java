package edu.anadolu.ltr;

public class InversedUrlLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        if (base.url.length() == 0)
            throw new RuntimeException("URL length is 0");
        return (double) 1 / base.url.length();
    }
}
