package edu.anadolu.freq;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;

import java.io.IOException;
import java.util.Arrays;

/**
 * Calculates term frequency distribution from Phi
 */
public class Phi extends TermFreqDistribution {

    private final NormalDistribution normalDistribution = new NormalDistribution();
    protected final IndexSearcher searcher;
    final long sumTotalTermFreq;
    final CollectionStatistics collectionStatistics;

    public Phi(IndexReader reader, BinningStrategy binningStrategy, String field, Analyzer analyzer) throws IOException {
        super(reader, binningStrategy, field, analyzer);
        this.searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();
        this.collectionStatistics = collectionStatistics;
        System.out.println("This index has " + sumTotalTermFreq + " many terms");
    }

    @Override
    public String outputFileName() {
        return field + "_phi_freq_" + binningStrategy.numBins() + ".csv";
    }

    /**
     * Total Term Frequency (TF) of a term
     */
    public long TF(Term term) throws IOException {
        TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
        return termStatistics.totalTermFreq();
    }

    @Override
    public String getTermDistributionStats(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) return word + "(stopword)";

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        final long TF = TF(term);

        // System.out.println(word + " has " + TF + " occurrences");

        int max = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final double tf = postingsEnum.freq();
            final long numTerms;

            if (norms.advanceExact(postingsEnum.docID())) {
                numTerms = norms.longValue();
            } else {
                numTerms = 0;
            }


            final double e_ij = ((double) (TF * numTerms)) / ((double) sumTotalTermFreq);

            double relativeFrequency = normalDistribution.cumulativeProbability((tf - e_ij) / Math.sqrt(e_ij));

            if (!(relativeFrequency >= 0 && relativeFrequency <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);


            final int value = binningStrategy.calculateBinValue(relativeFrequency);

            array[value]++;
            if (value > max)
                max = value;

        }

        return rollCountArray(max, array);
    }

    @Override
    public String toString() {
        return "Phi Term Freq. Distribution";
    }

}
