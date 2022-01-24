package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimKeywordH implements IDocFeature {

    String type="";

    public SimKeywordH(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.keyword, base.hTags);
        if("bert".equals(this.type))
            return base.bertSim(base.vectorkeyword,base.vectorhTags);
        return base.cosSim(String.join(" ",base.keyword),String.join(" ",base.hTags));
    }
}

