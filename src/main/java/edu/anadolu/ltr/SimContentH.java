package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.jsoup.select.Elements;

import java.io.IOException;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimContentH implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
        return base.textSimilarity(base.listContent, base.hTags);
    }
}
