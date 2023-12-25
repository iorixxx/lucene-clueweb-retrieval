package edu.anadolu.ltr;

import edu.anadolu.similarities.BM25;

import java.io.IOException;
import java.util.List;

public class CoveredTermCount implements IQDFeature {

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
        return base.listContent.contains(word)?1:0;
    }
}
