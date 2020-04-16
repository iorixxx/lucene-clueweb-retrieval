package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;

import java.io.IOException;
import java.util.Arrays;


public class SimTitleDescription implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        return base.textSimilarity(base.title, base.description);
//        return base.cosSim(String.join(" ",base.title),String.join(" ",base.description));
    }
}
