package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

public class JelinekMercerLM extends ModelBase {

    final private double lambda;

    public JelinekMercerLM(double lambda) {
        this.lambda = lambda;
    }

    public JelinekMercerLM() {
        this(0.5);
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {


        return
                log2(1 +
                        ((1 - lambda) * tf / docLength) /
                                (lambda * ((termFrequency) / (numberOfTokens))));
    }


    @Override
    public String toString() {


        return "JelinekMercerl" + lambda;

    }
}
