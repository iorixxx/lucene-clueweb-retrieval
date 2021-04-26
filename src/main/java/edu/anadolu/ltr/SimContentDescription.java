package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.solr.client.solrj.io.eval.CosineSimilarityEvaluator;

import java.io.IOException;
import java.util.Arrays;

public class SimContentDescription implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.listContent, base.description);
        return base.cosSim(String.join(" ",base.listContent),String.join(" ",base.description));
    }
}