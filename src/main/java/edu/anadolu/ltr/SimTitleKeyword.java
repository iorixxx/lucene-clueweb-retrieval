package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;

import java.io.IOException;
import java.util.Arrays;


public class SimTitleKeyword implements IDocFeature {

    String type="";

    public SimTitleKeyword(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.title, base.keyword);
        if("bert".equals(this.type))
            return base.bertSim(base.vectortitle,base.vectorkeyword);
        return base.cosSim(String.join(" ",base.title),String.join(" ",base.keyword));
    }
}
