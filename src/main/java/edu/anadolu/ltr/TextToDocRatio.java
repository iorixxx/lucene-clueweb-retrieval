package edu.anadolu.ltr;

public class TextToDocRatio implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String text = base.jDoc.text().replaceAll("\\s+", " ");
        String html = base.jDoc.html().replaceAll("\\s+", " ");
        return (double) (text.length()) / html.length();
    }
}
