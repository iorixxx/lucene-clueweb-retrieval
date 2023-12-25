package edu.anadolu.ltr;

import org.apache.solr.common.StringUtils;
import org.jsoup.select.Elements;

import java.util.function.Predicate;


public class HdensityToMax implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        return (double) base.hTags.size() / base.listContent.size();
    }
}