package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the DLH13 weighting model. This is a parameter-free
 * weighting model. Even if the user specifies a parameter value, it will <b>NOT</b>
 * affect the results. It is highly recommended to use the model with query expansion.
 * <p><b>References</b>
 * <ol>
 * <li>University of Glasgow at TREC 2005: Experiments in Terabyte and Enterprise
 * Tracks with Terrier. Craig Macdonald, Ben He, Vassilis Plachouras and Iadh Ounis.
 * In Proceedings of TREC 2005.</li>
 * <li>Frequentist and Bayesian approach to  Information Retrieval. G. Amati. In
 * Proceedings of the 28th European Conference on IR Research (ECIR 2006).
 * LNCS vol 3936, pages 13--24.</li>
 * </ol>
 */
public final class DLH13 extends ModelBase {

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double f = relativeFrequency(tf, docLength);
        return
                keyFrequency
                        * (tf * log2((tf * averageDocumentLength / docLength) *
                        (numberOfDocuments / termFrequency))
                        + 0.5d * log2(2d * Math.PI * tf * (1d - f)))
                        / (tf + DLH.k);
    }

    protected float score(BasicStats stats, float tf, float docLength) {

        double f = tf / docLength;
        double returnValue =

                (tf * log2((tf * stats.getAvgFieldLength() / docLength) *
                        ((double) stats.getNumberOfDocuments() / stats.getTotalTermFreq()))
                        + 0.5d * log2(2d * Math.PI * tf * (1d - f)))
                        / (tf + DLH.k);

        return (float) returnValue;

    }


    @Override
    public String toString() {
        return "DLH13";
    }
}