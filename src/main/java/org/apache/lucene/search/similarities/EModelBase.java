package org.apache.lucene.search.similarities;

import edu.anadolu.freq.TFNormalization;
import org.apache.commons.math3.util.Precision;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended version of {@link ModelBase} that has access the term being scored.
 */
public abstract class EModelBase extends Similarity implements Closeable {
    /**
     * For {@link #log2(double)}. Precomputed for efficiency reasons.
     */
    private static final double LOG_2 = Math.log(2);

    protected final String home;

    protected final String tag;

    protected final Connection conn;

    private static final String SELECT_COUNT = "SELECT count as DF FROM `w_%s` WHERE value=%s";

    protected final Map<String, Long> DFMap;

    public EModelBase(TFNormalization normalization, String home, String tag) throws Exception {

        this.home = home;
        this.tag = tag;
        final String dbName = tag + normalization.toString();
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName, "root", "");

        DFMap = new HashMap<>();
        Path fieldStats = Paths.get(home, "stats", tag, "contents_term_stats.csv");
        List<String> lines = Files.readAllLines(fieldStats, StandardCharsets.US_ASCII);
        for (String line : lines) {
            if ("term \t totalTermFreq \t docFreq".equals(line)) continue;

            String[] parts = line.split("\t");

            if (parts.length != 4)
                throw new RuntimeException("line from field_stats.csv does not have three parts " + line);

            long df = Long.parseLong(parts[2]);

            DFMap.put(parts[0], df);
        }

        System.out.println(DFMap);

    }

    @Override
    public void close() throws IOException {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException sql) {
            sql.printStackTrace();
        }
    }

    private long fetch(String sql) {
        long l = 0;
        try {

            Statement statement = conn.createStatement();

            ResultSet rs = statement.executeQuery(sql);
            if (rs.next())
                l = rs.getLong("DF");

            statement.close();
            rs.close();

        } catch (SQLException sqe) {
            sqe.printStackTrace();
        }

        return l;
    }

    /**
     * Calculates cumulative distribution function of Prob(X < tfn)
     *
     * @param term query term
     * @param tfn  normalized term frequency
     * @return probability between 1/numDocs and df/numDocs
     */
    protected long sqlCDF(String term, double tfn) {

        String sql = String.format(SELECT_COUNT, term, Double.toString(Precision.round(tfn, 4)));

        final long l = fetch(sql);

        if (l == 0) {
            throw new RuntimeException("{" + term + "}" + tfn);
        }

        return l;
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        EStats stats[] = new EStats[termStats.length];
        for (int i = 0; i < termStats.length; i++) {
            stats[i] = new EStats(collectionStats.field(), termStats[i].term().utf8ToString());
            fillBasicStats(stats[i], collectionStats, termStats[i]);
            //System.out.println(analyzer.toString() + "{" + termStats[i].term().utf8ToString() + "}");
        }
        return stats.length == 1 ? stats[0] : new MultiSimilarity.MultiStats(stats);
    }

    /**
     * Fills all member fields defined in {@code BasicStats} in {@code stats}.
     * Subclasses can override this method to fill additional stats.
     */
    protected void fillBasicStats(EStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
        // #positions(field) must be >= #positions(term)
        assert collectionStats.sumTotalTermFreq() == -1 || collectionStats.sumTotalTermFreq() >= termStats.totalTermFreq();
        long numberOfDocuments = collectionStats.docCount();

        long docFreq = termStats.docFreq();
        long totalTermFreq = termStats.totalTermFreq();

        // codec does not supply totalTermFreq: substitute docFreq
        if (totalTermFreq == -1) {
            totalTermFreq = docFreq;
        }

        final long numberOfFieldTokens;
        final double avgFieldLength;

        long sumTotalTermFreq = collectionStats.sumTotalTermFreq();

        if (sumTotalTermFreq <= 0) {
            // field does not exist;
            // We have to provide something if codec doesnt supply these measures,
            // or if someone omitted frequencies for the field... negative values cause
            // NaN/Inf for some scorers.
            numberOfFieldTokens = docFreq;
            avgFieldLength = 1;
        } else {
            numberOfFieldTokens = sumTotalTermFreq;
            avgFieldLength = (double) numberOfFieldTokens / numberOfDocuments;
        }

        stats.setNumberOfDocuments(numberOfDocuments);
        stats.setNumberOfFieldTokens(numberOfFieldTokens);
        stats.averageDocumentLength = avgFieldLength;
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
    private float score(EStats stats, float freq, long docLen) {

        /** The average length of documents in the collection.*/
        double averageDocumentLength = stats.averageDocumentLength;

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

        return (float) score(stats.term, freq, docLen, averageDocumentLength, keyFrequency, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

    }

    /**
     * @param term                  the query term
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
    protected abstract double score(String term,
                                    double tf, long docLength,
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
     * attaches the score (computed via the {@link #score(EStats, float, long)}
     * method) and the explanation for the term frequency.
     *
     * @param stats  the corpus level statistics.
     * @param doc    the document id.
     * @param freq   the term frequency and its explanation.
     * @param docLen the document length.
     * @return the explanation.
     */
    protected Explanation explain(EStats stats, int doc, Explanation freq, long docLen) {
        List<Explanation> subs = new ArrayList<>();
        return Explanation.match(
                score(stats, freq.getValue(), docLen),
                "score(" + getClass().getSimpleName() + ", doc=" + doc + ", docLen=" + docLen + ", freq=" + freq.getValue() + "), computed from:",
                subs);
    }

    @Override
    public SimScorer simScorer(SimWeight stats, LeafReaderContext context) throws IOException {
        if (stats instanceof MultiSimilarity.MultiStats) {
            // a multi term query (e.g. phrase). return the summation,
            // scoring almost as if it were boolean query
            SimWeight subStats[] = ((MultiSimilarity.MultiStats) stats).subStats;
            SimScorer subScorers[] = new SimScorer[subStats.length];
            for (int i = 0; i < subScorers.length; i++) {
                EStats eStats = (EStats) subStats[i];
                subScorers[i] = new BasicSimScorer(eStats, context.reader().getNormValues(eStats.field));
            }
            return new MultiSimilarity.MultiSimScorer(subScorers);
        } else {
            EStats basicstats = (EStats) stats;
            return new BasicSimScorer(basicstats, context.reader().getNormValues(basicstats.field));
        }
    }

    /**
     * Subclasses must override this method to return the name of the Similarity
     * and preferably the values of parameters (if any) as well.
     */
    @Override
    public abstract String toString();


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

    /**
     * Delegates the {@link #score(int, float)} and
     * {@link #explain(int, Explanation)} methods to
     * {@link SimilarityBase#score(BasicStats, float, float)} and
     * {@link SimilarityBase#explain(BasicStats, int, Explanation, float)},
     * respectively.
     */
    private class BasicSimScorer extends SimScorer {
        private final EStats stats;
        private final NumericDocValues norms;

        BasicSimScorer(EStats stats, NumericDocValues norms) throws IOException {
            this.stats = stats;
            this.norms = norms;
        }

        @Override
        public float score(int doc, float freq) {
            // We have to supply something in case norms are omitted
            return EModelBase.this.score(stats, freq,
                    norms == null ? 1L : norms.get(doc));
        }

        @Override
        public Explanation explain(int doc, Explanation freq) {
            return EModelBase.this.explain(stats, doc, freq,
                    norms == null ? 1L : norms.get(doc));
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

    private class EStats extends BasicStats {
        final String term;

        double averageDocumentLength;


        public EStats(String field, String term) {
            super(field);
            this.term = term;
        }
    }
}
