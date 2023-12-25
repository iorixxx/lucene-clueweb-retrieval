package edu.anadolu.ltr;

import org.jsoup.select.Elements;

public class OutLinkCount implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        //TODO investigate calculation method further
        Elements links = base.jDoc.select("a");
        if (links.size() == 0) return 0;
        int in = base.inlinkCount(base.jDoc, links);
        int out = links.size() - in;

        return (double) out;
    }
}