package edu.anadolu.ltr;

import edu.anadolu.similarities.DLH13;

import java.io.IOException;
import java.util.List;

public class WMWD_DLH13 implements IQDFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(QDFeatureBase base, String word, List<String> subParts) throws IOException {
        double keyFreq = 0.0;
        for(String part : subParts){
            if(part.equals(word))   keyFreq+=1.0;
        }
        long sumTotalTermFreq = base.collectionStatistics.sumTotalTermFreq();
        long docCount = base.collectionStatistics.docCount();

        if(base.tf <= 0) return 0;
        return new DLH13().score(base.tf,base.dl,(double) sumTotalTermFreq / docCount, keyFreq, base.termStatisticsMap.get(word).docFreq(), base.termStatisticsMap.get(word).totalTermFreq(),
                docCount,sumTotalTermFreq);
    }
}
