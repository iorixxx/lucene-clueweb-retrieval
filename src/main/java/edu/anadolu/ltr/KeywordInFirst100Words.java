package edu.anadolu.ltr;


import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.field.MetaTag;

import java.util.Arrays;
import java.util.List;

public class KeywordInFirst100Words implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String[] first100words = Arrays.copyOfRange(base.listContent.toArray(new String[0]),0,100);
            for (String token : first100words) {
                if (base.keyword.contains(token))
                    return 1;
            }
            return 0;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}
