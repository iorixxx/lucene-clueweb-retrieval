package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;


/*
Zhai, C., & Lafferty, J. (2004). A study of smoothing methods for language models applied to information retrieval. ACM Transactions on Information Systems (TOIS), 22(2), 179-214.
 */
public class LMJM extends ModelBase {

    final private double lambda;

    public LMJM(double lambda) {
        this.lambda = lambda;
    }

    public LMJM() {
        // Letor 3.0 default lambda = 0.1
        // 0.1 for short 0.7 for long queries on the paper
        this(0.1);
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {


        return
                log2(
                        ((1 - lambda) * tf / docLength) +
                                (lambda * ((termFrequency) / (numberOfTokens))));
    }


    @Override
    public String toString() {


        return "LMJM" + lambda;

    }
}
