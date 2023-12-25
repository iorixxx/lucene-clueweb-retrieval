package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 Zhai, C., & Lafferty, J. (2004). A study of smoothing methods for language models applied to information retrieval. ACM Transactions on Information Systems (TOIS), 22(2), 179-214.
 */
public class LMDIR extends ModelBase {


    final double c;

    public LMDIR(double c) {
        this.c = c;
    }

    public LMDIR()
    {
        // Letor 3.0 and the paper, default c = 2000
        this(2000);
    }


    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return log2((tf+c*(termFrequency/numberOfTokens))/
                (docLength+c));
    }

    @Override
    public String toString() {
        return "LMDIR" + c;
    }
}
