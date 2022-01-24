package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;

import java.io.IOException;
import java.util.Arrays;

public class SimContentKeyword implements IDocFeature {

    String type="";

    public SimContentKeyword(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.listContent, base.keyword);
        if("bert".equals(this.type))
            return base.bertSim(base.vectorlistContent,base.vectorkeyword);
        return base.cosSim(String.join(" ",base.listContent),String.join(" ",base.keyword));
    }
}
