package edu.anadolu.ltr;

import org.jsoup.select.Elements;


public class InOutlinkToAll implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        //TODO investigate calculation method further
        Elements links = base.jDoc.select("a");
        if (links.size() == 0) return 0;
        int inlink = base.inlinkCount(base.jDoc, links);
        int outlink = links.size()-inlink;

        return (double) (inlink-outlink) / links.size();
    }
}