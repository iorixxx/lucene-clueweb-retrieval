package edu.anadolu.similarities;


import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.NormalizationH2;

/**
 * Implements the Multi-Aspect Term Frequency (MATF)
 * <p>
 * <a href="http://doi.acm.org/10.1145/2484028.2484070">A novel TF-IDF weighting scheme for effective ranking</a>
 * </p>
 */
public final class MATF extends ModelBase {

    private int maxOverlap = -1;

    public void setMaxOverlap(int maxOverlap) {
        this.maxOverlap = maxOverlap;
    }

    public float coord(int overlap, int maxOverlap) {
        // System.out.println("maxOverlap = " + maxOverlap);
        // this.maxOverlap = maxOverlap;
        return 1f;
    }

    @Override
    public float score(BasicStats stats, float tf, long docLen) {

        if (maxOverlap == -1) throw new RuntimeException("maxOverlap is -1!");

        // System.out.println("term = " + stats.term + " maxOverlap = " + maxOverlap);

        long uniqueTerms = 1L; // TODO fix this save this statistics into norms

        Stats s = new Stats(stats, tf, docLen, uniqueTerms, maxOverlap);

        return (float) (s.TFF() * s.TDF());
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        throw new UnsupportedOperationException("this score method is not supported by multi-aspect TF");
    }

    @Override
    public String toString() {
        return "MATF";
    }

    /**
     * Sub-linear damping function that satisfy below four criteria:
     * <p>
     * f'(x) > 0
     * f''(x)< 0
     * asymptotically upper bounded to 1
     * vanishes at zero : f(0) = 0;
     */
    static double sublinear(double x) {
        return x / (1 + x);
    }

    private class Stats {

        private Stats(BasicStats stats, double tf, long docLength, long uniqueTerms, int queryLength) {

            /** The average length of documents in the collection.*/
            this.averageDocumentLength = stats.getAvgFieldLength();

            /** The term frequency in the query.*/
            this.keyFrequency = 1;

            /** The document frequency of the term in the collection.*/
            this.documentFrequency = stats.getDocFreq();

            /** The term frequency in the collection.*/
            this.termFrequency = stats.getTotalTermFreq();

            /** The number of documents in the collection.*/
            this.numberOfDocuments = stats.getNumberOfDocuments();

            /** The number of tokens in the collections. */
            this.numberOfTokens = stats.getNumberOfFieldTokens();

            this.tf = tf;
            this.uniqueTerms = uniqueTerms;
            this.docLength = docLength;
            this.queryLength = queryLength;
        }

        final double tf;
        final long uniqueTerms;
        final long docLength;
        final int queryLength;

        final double averageDocumentLength;
        final double keyFrequency;
        final double documentFrequency;
        final double termFrequency;
        final double numberOfDocuments;
        final double numberOfTokens;


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
            return log2(1 + tf) / log2(1 + avgTF());
        }


        /**
         * Average term frequency of the document being scored
         *
         * @return mean term frequency of the document that contains t
         */
        double avgTF() {
            return (double) docLength / uniqueTerms;
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
    }

    public static void main(String[] args) {

        double a = 5;

        double b = 43;

        double N = 1000;

        System.out.println(Math.log(a / N) / Math.log(b / N));

        System.out.println(Math.log(N / a) / Math.log(N / b));
    }
}
