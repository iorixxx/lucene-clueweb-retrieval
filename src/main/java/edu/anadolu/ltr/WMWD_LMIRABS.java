package edu.anadolu.ltr;

import edu.anadolu.similarities.LMABS;

import java.io.IOException;
import java.util.List;


public class WMWD_LMIRABS implements IQDFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(QDFeatureBase base, String word, List<String> subParts) throws IOException {
        // keyFrequency is used for uniqueTermCount only for Abs
        double keyFreq = base.uniqueDl;
        long sumTotalTermFreq = base.collectionStatistics.sumTotalTermFreq();
        long docCount = base.collectionStatistics.docCount();
        return new LMABS().score(base.tf,base.dl,(double) sumTotalTermFreq / docCount, keyFreq, base.termStatisticsMap.get(word).docFreq(), base.termStatisticsMap.get(word).totalTermFreq(),
                docCount,sumTotalTermFreq);
    }
}
