package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimContentH implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        return base.textSimilarity(base.listContent, base.hTags);
//        return base.cosSim(String.join(" ",base.listContent),String.join(" ",base.hTags));
    }
}
