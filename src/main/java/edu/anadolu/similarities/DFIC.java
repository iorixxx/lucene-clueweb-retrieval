package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * Divergence From Independence model based on Chi-square statistics
 * (i.e., standardized Chi-squared distance from independence in term frequency tf).
 * <p>
 * For more information:
 * <p>
 * A Nonparametric Term Weighting Method for Information Retrieval Based on Measuring the Divergence from Independence.
 * Kocabas, Dincer & Karaoglan, International Journal of Information Retrieval, 17(2), 2014.
 * doi: http://dx.doi.org/10.1007/s10791-013-9225-4.
 * <p>
 */
public final class DFIC extends ModelBase {


    protected float score(BasicStats stats, float freq, float docLen) {

        // System.out.println(stats.toString() + "\t" + stats.getTotalTermFreq());
        double e_ij = (stats.getTotalTermFreq() * docLen) / stats.getNumberOfFieldTokens();

        // Condition 1
        if (freq <= e_ij) return 0F;

        double chiSquare = (Math.pow((freq - e_ij), 2) / e_ij) + 1;

        return (float) log2(chiSquare);
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double e_ij = (termFrequency * docLength) / numberOfTokens;

        // Condition 1
        if (tf <= e_ij) return 0D;

        double chiSquare = (Math.pow((tf - e_ij), 2) / e_ij) + 1;

        return keyFrequency * log2(chiSquare);
    }

    @Override
    public String toString() {
        return "DFIC";
    }
}
