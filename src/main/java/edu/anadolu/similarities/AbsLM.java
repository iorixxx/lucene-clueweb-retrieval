package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

public class AbsLM  extends ModelBase {

    final private double sigma;

    public AbsLM(double sigma) {
        this.sigma = sigma;
    }

    public AbsLM() {
        this(0.7);
    }

    /*
        Code has been copied from https://github.com/Endmaril/LMAbsSimilarity
        deltas can be replaced with sigma 0.7 for default
     */
    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {


        //TODO search for better implementations

        //float score = stats.getBoost() * (float)(Math.log(1 + Math.max(freq - delta, 0) / (delta * ((LMStats)stats).getCollectionProbability() * uniqueTermCount)));
        //return score > 0.0f ? score : 0.0f;


        return 0;
    }


    @Override
    public String toString() {


        return "AbsLMs" + sigma;

    }
}
