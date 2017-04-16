package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.NormalizationH2;

import static edu.anadolu.similarities.MATF.sublinear;

/**
 * Implements the probabilistic model based on the Maximum Value Distribution (MVD)
 * <p>
 * <a href="http://doi.acm.org/10.1145/2766462.2767762">A Probabilistic Model for Information Retrieval Based on Maximum Value Distribution</a>
 */
public final class MVD extends ModelBase {

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return 0;
    }

    @Override
    public String toString() {
        return "MVD";
    }


    /**
     * Gumbel distribution
     */
    private static double Fg(double x, double standardDeviation) {
        double alpha = 2.5 + 0.04 * standardDeviation;
        return Math.exp(-Math.exp(-x / alpha));
    }

    /**
     * Frechet distribution
     */
    private static double Ff(double x, double alpha, double mu) {
        return Math.exp(-Math.pow((mu / x), alpha));
    }


    private class Stats {

        double tf;
        long docLength;
        double averageDocumentLength;
        double keyFrequency;
        double documentFrequency;
        double termFrequency;
        double numberOfDocuments;
        double numberOfTokens;


        int docID;
        int queryLength;


        /**
         * Term specific parameters: Gumbel
         */
        double standardDeviation;

        /**
         * Estimated parameters for Frechet
         */
        double alpha;
        double mu;


        /**
         * free-parameters
         */

        double beta = 1.0; // beta > 0

        double interpolation = 0.5; // (0,1) set empirically

        double G(double x) {
            double p = beta * IDF() / (1 + beta * IDF());
            return p * Fg(x, standardDeviation) + (1 - p) * Ff(x, alpha, mu);
        }


        /**
         * Length Regularized Term Frequency (LRTF)
         *
         * @see NormalizationH2#tfn(org.apache.lucene.search.similarities.BasicStats, float, float)
         */
        double LRTF() {
            return tf * log2(1 + averageDocumentLength / docLength);
        }


        /**
         * Relative Intra-document Term Frequency (RITF)
         */
        double RITF() {
            return log2(1 + tf) / log2(1 + avgTF(docID));
        }


        /**
         * Average term frequency of the document being scored
         *
         * @param docID internal Lucene document identifier
         * @return mean term frequency of the document that contains t
         */
        double avgTF(int docID) {
            return (double) docLength / numberOfUniqueTerms(docID);
        }

        long numberOfUniqueTerms(int docID) {
            return -1;
        }

        /**
         * Tends to prefer short documents.
         * Should be preferred for long queries.
         */
        double BLRTF() {
            return sublinear(LRTF());
        }


        /**
         * Tends to prefer long documents.
         * Should be preferred for short queries.
         */
        double BRITF() {
            return sublinear(RITF());
        }

        /**
         * Inverse Document Frequency (IDF)
         */
        double IDF() {
            return log2((numberOfDocuments + 1) / documentFrequency);
        }

        /**
         * Query Length Factor (QLF)
         *
         * @param queryLength number of words in the query
         * @return query length factor
         */
        double QLF(int queryLength) {
            return 2 / (1 + log2(1 + queryLength));
        }

        /**
         * Average Elite Set Term Frequency (AEF)
         */
        double AEF() {
            return termFrequency / documentFrequency;
        }

        /**
         * Term Discrimination Factor (TDF)
         */
        double TDF() {
            return IDF() * sublinear(AEF());
        }

        /**
         * Term Frequency Factor (TFF)
         */
        double TFF() {
            double w = QLF(queryLength);
            return w * BRITF() + (1 - w) * BLRTF();
        }

        double mvd() {
            return (interpolation * G(RITF()) + (1 - interpolation) * G(LRTF())) * IDF();
        }
    }
}
