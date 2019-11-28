package edu.anadolu.ltr;

public class TextToDocRatio implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String text = base.jDoc.text().replaceAll("\\s+", " ");
        String html = base.rawHTML.replaceAll("\\s+", " ");
        if(html.length()==0) return 0;
        return (double) (text.length()) / html.length();
    }
}
