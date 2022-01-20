package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;

import java.io.IOException;
import java.util.Arrays;


public class SimKeywordDescription implements IDocFeature {

    String type="";

    public SimKeywordDescription(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.keyword, base.description);
        if("bert".equals(this.type))
            return base.bertSim(String.join(" ",base.keyword),String.join(" ",base.description));
        return base.cosSim(String.join(" ",base.keyword),String.join(" ",base.description));
    }
}
