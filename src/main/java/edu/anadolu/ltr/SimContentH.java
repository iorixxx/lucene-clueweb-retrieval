package edu.anadolu.ltr;


import edu.anadolu.field.MetaTag;
import org.apache.commons.text.similarity.CosineDistance;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

import static edu.anadolu.field.MetaTag.notEmpty;

public class SimContentH implements IDocFeature {

    String type="";

    public SimContentH(String type){
        this.type=type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + this.type;
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException, NullPointerException {
//        return base.textSimilarity(base.listContent, base.hTags);
        if("bert".equals(this.type))
            return base.bertSim(base.vectorlistContent,base.vectorhTags);
        return base.cosSim(String.join(" ",base.listContent),String.join(" ",base.hTags));
    }
}
