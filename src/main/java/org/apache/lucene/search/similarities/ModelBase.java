package org.apache.lucene.search.similarities;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@link Similarity} that is useful for importing
 * Terrier 4.0 models into Lucene 5.2.0 in a copy-paste fashion.
 */
public abstract class ModelBase extends Similarity {
    /**
     * For {@link #log2(double)}. Precomputed for efficiency reasons.
     */
    private static final double LOG_2 = Math.log(2);

    /**
     * Sole constructor. (For invocation by subclass
     * constructors, typically implicit.)
     */
    public ModelBase() {
    }

    /**
     * Computes relative term frequency.
     * When tf == docLength we return 0.99999 because relative frequency of 1 produces
     * Not a Number or Negative Infinity as scores in hyper-geometric models (DPH, DLH and DLH13).
     *
     * @param tf        raw term frequency
     * @param docLength length of the document
     * @return relative term frequency
     */
    protected double relativeFrequency(double tf, long docLength) {
        assert tf <= docLength : "tf cannot be greater than docLength";
        double f = tf < docLength ? tf / docLength : 0.99999;
        assert f > 0 : "relative frequency must be greater than zero: " + f;
        assert f < 1 : "relative frequency must be less than one: " + f;
        return f;
    }

    @Override
    public final SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        BasicStats stats[] = new BasicStats[termStats.length];
        for (int i = 0; i < termStats.length; i++) {
            stats[i] = newStats(collectionStats.field(), boost);
            fillBasicStats(stats[i], collectionStats, termStats[i]);
        }
        return stats.length == 1 ? stats[0] : new MultiSimilarity.MultiStats(stats);
    }

    /**
     * Factory method to return a custom stats object
     */
    protected BasicStats newStats(String field, float boost) {
        return new BasicStats(field, boost);
    }

    /**
     * Fills all member fields defined in {@code BasicStats} in {@code stats}.
     * Subclasses can override this method to fill additional stats.
     */
    protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
        // #positions(field) must be >= #positions(term)
        assert collectionStats.sumTotalTermFreq() == -1 || collectionStats.sumTotalTermFreq() >= termStats.totalTermFreq();
        long numberOfDocuments = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();

        long docFreq = termStats.docFreq();
        long totalTermFreq = termStats.totalTermFreq();

        // frequencies are omitted, all postings have tf=1, so totalTermFreq = docFreq
        if (totalTermFreq == -1) {
            totalTermFreq = docFreq;
        }

        final long numberOfFieldTokens;
        final float avgFieldLength;

        if (collectionStats.sumTotalTermFreq() == -1) {
            // frequencies are omitted, so sumTotalTermFreq = # postings
            if (collectionStats.sumDocFreq() == -1) {
                // theoretical case only: remove!
                numberOfFieldTokens = docFreq;
                avgFieldLength = 1f;
            } else {
                numberOfFieldTokens = collectionStats.sumDocFreq();
                avgFieldLength = (float) (collectionStats.sumDocFreq() / (double) numberOfDocuments);
            }
        } else {
            numberOfFieldTokens = collectionStats.sumTotalTermFreq();
            avgFieldLength = (float) (collectionStats.sumTotalTermFreq() / (double) numberOfDocuments);
        }

        // TODO: add sumDocFreq for field (numberOfFieldPostings)
        stats.setNumberOfDocuments(numberOfDocuments);
        stats.setNumberOfFieldTokens(numberOfFieldTokens);
        stats.setAvgFieldLength(avgFieldLength);
        stats.setDocFreq(docFreq);
        stats.setTotalTermFreq(totalTermFreq);
    }

    /**
     * Scores the document {@code doc}.
     * <p>Subclasses must apply their scoring formula in this class.</p>
     *
     * @param stats  the corpus level statistics.
     * @param freq   the term frequency.
     * @param docLen the document length.
     * @return the score.
     */
    protected float score(BasicStats stats, float freq, long docLen) {

        /** The average length of documents in the collection.*/
        double averageDocumentLength = stats.getAvgFieldLength();

        /** The term frequency in the query.*/
        double keyFrequency = 1;

        /** The document frequency of the term in the collection.*/
        double documentFrequency = stats.getDocFreq();

        /** The term frequency in the collection.*/
        double termFrequency = stats.getTotalTermFreq();

        /** The number of documents in the collection.*/
        double numberOfDocuments = stats.getNumberOfDocuments();

        /** The number of tokens in the collections. */
        double numberOfTokens = stats.getNumberOfFieldTokens();

        double score = score(freq, docLen, averageDocumentLength, keyFrequency, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

        if (Double.isNaN(score) || Double.isInfinite(score)) {
            System.out.println("tf: " + freq);
            System.out.println("docLen: " + docLen);
            System.out.println(stats);
        }
        // This collector cannot handle these scores:
        assert score != Double.NEGATIVE_INFINITY : "This collector cannot handle score of Double.NEGATIVE_INFINITY coming from: " + this.getClass().getSimpleName();
        assert !Double.isNaN(score) : "This collector cannot handle score of Double.NaN coming from: " + this.getClass().getSimpleName();

        return (float) score;
    }

    /**
     * Score calculation for experiments
     *
     * @param tf                the term frequency.
     * @param docLength         the document length.
     * @param documentFrequency the document frequency of the term in the collection.
     * @param termFrequency     the term frequency in the collection.
     * @param numberOfDocuments the number of documents in the collection.
     * @param numberOfTokens    the number of tokens in the collections.
     * @return the score.
     */
    public double f(int tf, long docLength,
                    double documentFrequency,
                    double termFrequency,
                    double numberOfDocuments,
                    double numberOfTokens) {
        return score(tf, docLength, numberOfTokens / numberOfDocuments, 1, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
    }

    /**
     * @param tf                    the term frequency.
     * @param docLength             the document length.
     * @param averageDocumentLength the average length of documents in the collection.
     * @param keyFrequency          the term frequency in the query.
     * @param documentFrequency     the document frequency of the term in the collection.
     * @param termFrequency         the term frequency in the collection.
     * @param numberOfDocuments     the number of documents in the collection.
     * @param numberOfTokens        the number of tokens in the collections.
     * @return the score.
     */
    public abstract double score(double tf, long docLength,
                                 double averageDocumentLength,
                                 double keyFrequency,
                                 double documentFrequency,
                                 double termFrequency,
                                 double numberOfDocuments,
                                 double numberOfTokens);


    /**
     * Explains the score. The implementation here provides a basic explanation
     * in the format <em>score(name-of-similarity, doc=doc-id,
     * freq=term-frequency), computed from:</em>, and
     * attaches the score (computed via the {@link #score(BasicStats, float, long)}
     * method) and the explanation for the term frequency.
     *
     * @param stats  the corpus level statistics.
     * @param doc    the document id.
     * @param freq   the term frequency and its explanation.
     * @param docLen the document length.
     * @return the explanation.
     */
    protected Explanation explain(BasicStats stats, int doc, Explanation freq, long docLen) {
        List<Explanation> subs = new ArrayList<>();
        return Explanation.match(
                score(stats, freq.getValue(), docLen),
                "score(" + getClass().getSimpleName() + ", doc=" + doc + ", docLen=" + docLen + ", freq=" + freq.getValue() + "), computed from:",
                subs);
    }

    @Override
    public final SimScorer simScorer(SimWeight stats, LeafReaderContext context) throws IOException {

        if (stats instanceof MultiSimilarity.MultiStats) {
            // a multi term query (e.g. phrase). return the summation,
            // scoring almost as if it were boolean query
            SimWeight subStats[] = ((MultiSimilarity.MultiStats) stats).subStats;
            SimScorer subScorers[] = new SimScorer[subStats.length];
            for (int i = 0; i < subScorers.length; i++) {
                BasicStats basicstats = (BasicStats) subStats[i];
                subScorers[i] = new BasicSimScorer(basicstats, context.reader().getNormValues(basicstats.field));
            }
            return new MultiSimilarity.MultiSimScorer(subScorers);
        } else {
            BasicStats basicstats = (BasicStats) stats;
            return new BasicSimScorer(basicstats, context.reader().getNormValues(basicstats.field));
        }
    }

    /**
     * Subclasses must override this method to return the name of the Similarity
     * and preferably the values of parameters (if any) as well.
     */
    @Override
    public abstract String toString();

    @Override
    public boolean equals(Object object) {

        if (object == null) return false;
        if (this == object) return true;
        if (!(object instanceof ModelBase)) return false;

        return toString().equals(object.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }


    /**
     * Encodes the document length in a lossless way
     */
    @Override
    public long computeNorm(FieldInvertState state) {
        return state.getLength() - state.getNumOverlap();
    }

    // ----------------------------- Static methods ------------------------------

    /**
     * Returns the base two logarithm of {@code x}.
     */
    public static double log2(double x) {
        // Put this to a 'util' class if we need more of these.
        return Math.log(x) / LOG_2;
    }

    // --------------------------------- Classes ---------------------------------


    final class BasicSimScorer extends SimScorer {
        private final BasicStats stats;
        private final NumericDocValues norms;


        BasicSimScorer(BasicStats stats, NumericDocValues norms) {
            this.stats = stats;
            this.norms = norms;
        }

        long getLengthValue(int doc) throws IOException {
            if (norms == null) {
                throw new RuntimeException("document length norms is null!");
            }
            if (norms.advanceExact(doc)) {
                return norms.longValue();
            } else {
                throw new RuntimeException("norms.advanceExact(doc) returns false!");
            }
        }

        @Override
        public float score(int doc, float freq) throws IOException {
            // We have to supply something in case norms are omitted
            return ModelBase.this.score(stats, freq, getLengthValue(doc));
        }

        @Override
        public Explanation explain(int doc, Explanation freq) throws IOException {
            return ModelBase.this.explain(stats, doc, freq, getLengthValue(doc));
        }

        @Override
        public float computeSlopFactor(int distance) {
            return 1.0f / (distance + 1);
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            return 1f;
        }
    }
}
