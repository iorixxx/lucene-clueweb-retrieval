package edu.anadolu.ltr;

import org.apache.commons.text.similarity.CosineDistance;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

import static edu.anadolu.field.MetaTag.notEmpty;


public class SimTitleH implements IDocFeature {

    String type="";

    public SimTitleH(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.title, base.hTags);
        if("bert".equals(this.type))
            return base.bertSim(String.join(" ",base.title),String.join(" ",base.hTags));
        return base.cosSim(String.join(" ",base.title),String.join(" ",base.hTags));
    }
}

