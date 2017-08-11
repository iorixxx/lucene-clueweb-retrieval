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

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        int counter1 = 0;
        int max = 0;
        if (postingsEnum != null)
            while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

                final int freq = postingsEnum.freq() + 1;
                final long numTerms = norms.get(postingsEnum.docID()) + 1;
                final double relativeFrequency = (double) freq / (double) numTerms;

                if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                    throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

                final int value = binningStrategy.calculateBinValue(relativeFrequency);

                array[value]++;
                if (value > max)
                    max = value;

                counter1++;
            }


        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new MatchAllDocsQuery(), BooleanClause.Occur.FILTER)
                .add(new TermQuery(term), BooleanClause.Occur.MUST_NOT);

        ScoreDoc[] hits = searcher.search(builder.build(), Integer.MAX_VALUE).scoreDocs;

        if ((counter1 + hits.length) != reader.numDocs()) {
            System.out.println("term enum : " + counter1 + " filter clause : " + hits.length);
            System.out.println("docCount : " + collectionStatistics.docCount() + " sum : " + Integer.toString(counter1 + hits.length));
        }

        for (ScoreDoc scoreDoc : hits) {
            int docId = scoreDoc.doc;
            final long numTerms = norms.get(docId) + 1;
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
