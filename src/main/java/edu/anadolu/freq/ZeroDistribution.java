package edu.anadolu.freq;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.Arrays;

public class ZeroDistribution extends Phi {

    public ZeroDistribution(IndexReader reader, BinningStrategy binningStrategy, String field, Analyzer analyzer) throws IOException {
        super(reader, binningStrategy, field, analyzer);
    }

    @Override
    public String outputFileName() {
        return field + "_zero_freq_" + binningStrategy.numBins() + ".csv";
    }

    public String getTermDistributionStats(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) return word + "(stopword)";

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        int max = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final int freq = postingsEnum.freq() + 1;
            final long numTerms = norms.get(postingsEnum.docID());
            final double relativeFrequency = (double) freq / (double) numTerms;

            if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

            final int value = binningStrategy.calculateBinValue(relativeFrequency);

            array[value]++;
            if (value > max)
                max = value;

        }


        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)
                .add(new TermQuery(term), BooleanClause.Occur.FILTER);

        ScoreDoc[] hits = searcher.search(new ConstantScoreQuery(builder.build()), Integer.MAX_VALUE).scoreDocs;

        for (ScoreDoc scoreDoc : hits) {
            int docId = scoreDoc.doc;
            final long numTerms = norms.get(docId);
            final double relativeFrequency = 1.0d / (double) numTerms;

            if (!(relativeFrequency > 0 && relativeFrequency <= 1))
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
        return "Zero Term Freq. Distribution";
    }

}
