package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * Divergence From Independence model based on Standardization
 * (i.e., standardized distance from independence in term frequency tf).
 * <p>
 * For more information:
 * <p>
 * A Nonparametric Term Weighting Method for Information Retrieval Based on Measuring the Divergence from Independence.
 * Kocabas, Dincer & Karaoglan, International Journal of Information Retrieval, 17(2), 2014.
 * doi: http://dx.doi.org/10.1007/s10791-013-9225-4.
 * <p>
 */
public final class DFIZ extends ModelBase {

    protected float score(BasicStats stats, float freq, float docLen) {

        /** termFrequency : The term frequency in the collection.*/
        /** numberOfTokens : The number of tokens in the collections. */

        // double  e_ij = (termFrequency * docLength) / numberOfTokens;


        double e_ij = (stats.getTotalTermFreq() * docLen) / stats.getNumberOfFieldTokens();

        // Condition 1
        if (freq <= e_ij) return 0F;

        double zScore = ((freq - e_ij) / Math.sqrt(e_ij)) + 1;

        return (float) log2(zScore);
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double e_ij = (termFrequency * docLength) / numberOfTokens;

        // Condition 1
        if (tf <= e_ij) return 0D;

        double zScore = ((tf - e_ij) / Math.sqrt(e_ij)) + 1;

        return keyFrequency * log2(zScore);
    }

    @Override
    public String toString() {
        return "DFIZ";
    }
}
