package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the DFRee weighting model. DFRee stands for DFR free from parameters.
 * In particular, the DFRee model computes an average number of extra bits (as information
 * divergence) that are necessary to code one extra token of the query term with respect to
 * the probability distribution observed in the document. There are two possible populations
 * to sample the probability distribution: considering only the document and no other document
 * in the collection, or the document considered as sample drawn from the entire collection
 * statistics. DFRee takes an average of these two information measures, that is their inner product.
 */
public final class DFRee extends ModelBase {

    protected float score(BasicStats stats, float tf, float docLength) {

        /**
         * DFRee model with the log normalisation function.
         */
        double prior = tf / docLength;
        double posterior = (tf + 1d) / (docLength + 1);
        double InvPriorCollection = stats.getNumberOfFieldTokens() / stats.getTotalTermFreq();
        //double alpha = 1d/docLength; //0 <= alpha <= posterior


        double norm = tf * log2(posterior / prior);

        double returnValue = norm * (
                tf * (
                        -log2(prior * InvPriorCollection)
                )
                        +
                        (tf + 1d) * (
                                +log2(posterior * InvPriorCollection)
                        )
                        + 0.5 * log2(posterior / prior)
        );


        return (float) returnValue;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        /**
         * DFRee model with the log normalisation function.
         */
        double prior = tf / docLength;
        double posterior = (tf + 1d) / (docLength + 1);
        double InvPriorCollection = numberOfTokens / termFrequency;
        //double alpha = 1d/docLength; //0 <= alpha <= posterior


        double norm = tf * log2(posterior / prior);

        return keyFrequency * norm * (
                tf * (-log2(prior * InvPriorCollection)
                )
                        +
                        (tf + 1d) * (
                                +log2(posterior * InvPriorCollection)
                        )
                        + 0.5 * log2(posterior / prior)
        );
    }

    @Override
    public String toString() {
        return "DFRee";
    }
}
