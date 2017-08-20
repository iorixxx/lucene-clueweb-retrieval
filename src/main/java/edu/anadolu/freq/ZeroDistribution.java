package edu.anadolu.freq;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.Arrays;

import static edu.anadolu.cmdline.CmdLineTool.execution;

public class ZeroDistribution extends Phi {

    private final int[] allDocIDs;
    private static final String ARTIFICIAL_FIELD = "all";
    private static final Term ARTIFICIAL_TERM = new Term(ARTIFICIAL_FIELD, ARTIFICIAL_FIELD);

    public ZeroDistribution(IndexReader reader, BinningStrategy binningStrategy, String field, Analyzer analyzer) throws IOException {

        super(reader, binningStrategy, field, analyzer);

        final long start = System.nanoTime();

        allDocIDs = new int[reader.numDocs()];

        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, ARTIFICIAL_FIELD, ARTIFICIAL_TERM.bytes());

        if (postingsEnum == null) {
            throw new RuntimeException("artificial token should exists in every document!");
        }

        int i = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            if (postingsEnum.freq() != 1)
                throw new RuntimeException("artificial term frequency should be 1! " + postingsEnum.freq());

            allDocIDs[i++] = postingsEnum.docID();
        }

        System.out.println("All " + Integer.toString(i) + " docIDs are obtained in " + execution(start));

        Arrays.sort(allDocIDs);
    }

    @Override
    public String outputFileName() {
        return field + "_zero_freq_" + binningStrategy.numBins() + ".csv";
    }

    public String getTermDistributionStatsSlow(String word) throws IOException {

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
    public String getTermDistributionStats(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        int counter = 0;
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

                counter++;
            }

        PostingsEnum first = MultiFields.getTermDocsEnum(reader, ARTIFICIAL_FIELD, ARTIFICIAL_TERM.bytes());

        if (first == null) {
            throw new RuntimeException("artificial token should exists in every document!");
        }

        int firstDocId = first.nextDoc();

        PostingsEnum second = MultiFields.getTermDocsEnum(reader, field, term.bytes());
        int secondDocId = (second == null ? PostingsEnum.NO_MORE_DOCS : second.nextDoc());

        // We are assuming that docEnum are in doc id order
        // According to Lucene documentation, the doc ids are in non decreasing order
        while (true) {

            if (firstDocId == PostingsEnum.NO_MORE_DOCS || secondDocId == PostingsEnum.NO_MORE_DOCS) {
                break;
            }

            if (firstDocId < secondDocId) {

                counter++;

                if (first.freq() != 1)
                    throw new RuntimeException("artificial term frequency should be 1! " + first.freq());

                final int docId = first.docID();

                final long numTerms;
                try {
                    numTerms = norms.get(docId) + 1;
                } catch (Exception e) {
                    System.out.println("exception occurred for " + word + " " + e.toString());
                    firstDocId = first.nextDoc();
                    continue;
                }
                //  if (numTerms == 1L) System.out.println("document length zero");
                final double relativeFrequency = 1.0d / (double) numTerms;

                if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                    throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

                final int value = binningStrategy.calculateBinValue(relativeFrequency);

                array[value]++;
                if (value > max)
                    max = value;

                firstDocId = first.nextDoc();

            } else if (firstDocId > secondDocId) {

                secondDocId = second.nextDoc();

            } else {

                firstDocId = first.nextDoc();
                secondDocId = second.nextDoc();
            }
        }


        while (firstDocId != PostingsEnum.NO_MORE_DOCS) {

            counter++;

            if (first.freq() != 1) throw new RuntimeException("artificial term frequency should be 1! " + first.freq());
            final int docId = first.docID();

            final long numTerms = norms.get(docId) + 1;
            final double relativeFrequency = 1.0d / (double) numTerms;

            if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

            final int value = binningStrategy.calculateBinValue(relativeFrequency);

            array[value]++;
            if (value > max)
                max = value;

            firstDocId = first.nextDoc();
        }


        if (counter != reader.numDocs()) {
            System.out.println("term enum : " + counter + " filter clause : " + reader.numDocs());
            System.out.println("docCount : " + collectionStatistics.docCount());
        }

        return rollCountArray(max, array);
    }


    @Override
    public String toString() {
        return "Zero Term Freq. Distribution";
    }

}
