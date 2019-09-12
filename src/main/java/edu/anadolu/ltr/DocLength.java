package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.solr.common.StringUtils;

public class DocLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        return Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(Tag.KStem)).size();
    }
}
