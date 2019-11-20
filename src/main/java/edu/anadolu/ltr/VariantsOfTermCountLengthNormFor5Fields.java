package edu.anadolu.ltr;

import java.io.IOException;
import java.util.List;

public class VariantsOfTermCountLengthNormFor5Fields implements IQDFeature{
    
    @Override
    public QDFeatureType type(){
        return QDFeatureType.ALL;
    }

    @Override
    public QDFeatureFields field(){
        return QDFeatureFields.ALL;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(QDFeatureBase base, String word, List<String> subParts) throws IOException {
        return base.tf/base.listContent.size();
    }
}
