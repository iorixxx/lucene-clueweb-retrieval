package edu.anadolu.ltr;

import java.io.IOException;
import java.util.List;

public class MinCoverageForTitle implements IQDFeature {

    @Override
    public QDFeatureType type(){
        return QDFeatureType.DIFF;
    }


    @Override
    public QDFeatureFields field(){
        return QDFeatureFields.TITLE;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(QDFeatureBase base, String word, List<String> subParts) throws IOException {
        return base.listContent.contains(word)?base.listContent.indexOf(word):-1;
    }
}
