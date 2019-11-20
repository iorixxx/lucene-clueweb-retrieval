package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;


/*
Zhai, C., & Lafferty, J. (2004). A study of smoothing methods for language models applied to information retrieval. ACM Transactions on Information Systems (TOIS), 22(2), 179-214.
 */

public class LMABS extends ModelBase {

    final private double delta;

    public LMABS(double delta) {
        this.delta = delta;
    }

    public LMABS() {
        // Letor 3.0 and paper, default sigma = 0.7
        this(0.7);
    }

    /*
        Code has been copied from https://github.com/Endmaril/LMAbsSimilarity
        deltas can be replaced with sigma 0.7 for default
        float score = stats.getBoost() * (float)(Math.log(1 + Math.max(freq - delta, 0) / (delta * ((LMStats)stats).getCollectionProbability() * uniqueTermCount)));
        return score > 0.0f ? score : 0.0f;
     */
    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {

        // keyFrequency is used for uniqueTermCount only for Abs
        return log2((Math.max((tf- delta),0)/docLength) +
                ((delta *keyFrequency)/docLength) * (termFrequency/numberOfTokens));
        
    }


    @Override
    public String toString() {
        return "LMABS" + delta;

    }
}
