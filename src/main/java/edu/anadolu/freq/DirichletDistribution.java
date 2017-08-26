package edu.anadolu.freq;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.Arrays;

public class DirichletDistribution extends ZeroDistribution {

    private final double mu;

    public DirichletDistribution(IndexReader reader, BinningStrategy binningStrategy, String field, Analyzer analyzer) throws IOException {
        super(reader, binningStrategy, field, analyzer);
        mu = (double) sumTotalTermFreq / collectionStatistics.docCount();
        System.out.println("avdl = " + mu);
    }

    @Override
    public String outputFileName() {
        return field + "_dirichlet_freq_" + binningStrategy.numBins() + ".csv";
    }

    @Override
    public String toString() {
        return "Dirichlet Smoothing Term Freq. Distribution";
    }

    @Override
    public String getTermDistributionStats(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        final long TF = TF(term);

        int counter = 0;
        int max = 0;
        if (postingsEnum != null)
            while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

                final int tf = postingsEnum.freq();
                final long docLen = norms.get(postingsEnum.docID());

                if (tf < 1 || docLen < 1) throw new RuntimeException("tf or docLen is less than one!");

//                double relativeFrequency = (double) TF / sumTotalTermFreq;
//                relativeFrequency *= mu;
//                relativeFrequency += (double) tf;
//                relativeFrequency /= (mu + (double) docLen);


                double relativeFrequency = relativeFrequency(tf, docLen, TF);

                if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                    throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

                final int value = binningStrategy.calculateBinValue(relativeFrequency);

                counter++;
                array[value]++;
                if (value > max)
                    max = value;


            }

        PostingsEnum allDocsEnum = MultiFields.getTermDocsEnum(reader, ARTIFICIAL_FIELD, ARTIFICIAL_TERM.bytes());

        if (allDocsEnum == null) {
            throw new RuntimeException("artificial token should exists in every document!");
        }

        int firstDocId = allDocsEnum.nextDoc();

        PostingsEnum second = MultiFields.getTermDocsEnum(reader, field, term.bytes());
        int secondDocId = (second == null ? PostingsEnum.NO_MORE_DOCS : second.nextDoc());

        // We are assuming that docEnum are in doc id order
        // According to Lucene documentation, the doc ids are in non decreasing order
        while (true) {

            if (firstDocId == PostingsEnum.NO_MORE_DOCS || secondDocId == PostingsEnum.NO_MORE_DOCS) {
                break;
            }

            if (firstDocId < secondDocId) {

                final int value = bin(allDocsEnum, TF);
                if (-1 == value) {
                    firstDocId = allDocsEnum.nextDoc();
                    continue;
                }

                counter++;
                array[value]++;
                if (value > max)
                    max = value;

                firstDocId = allDocsEnum.nextDoc();

            } else if (firstDocId > secondDocId) {

                secondDocId = second.nextDoc();

            } else {

                firstDocId = allDocsEnum.nextDoc();
                secondDocId = second.nextDoc();
            }
        }


        while (firstDocId != PostingsEnum.NO_MORE_DOCS) {

            final int value = bin(allDocsEnum, TF);

            if (-1 == value) {
                firstDocId = allDocsEnum.nextDoc();
                continue;
            }

            counter++;
            array[value]++;
            if (value > max)
                max = value;

            firstDocId = allDocsEnum.nextDoc();
        }


        if (counter != collectionStatistics.docCount()) {
            System.out.println("counter : " + counter + " docCount : " + collectionStatistics.docCount() + " reader.numDocs : " + reader.numDocs());
        }

        return rollCountArray(max, array);
    }

    public int bin(PostingsEnum allDocsEnum, long TF) throws IOException {

        if (allDocsEnum.freq() != 1)
            throw new RuntimeException("artificial term frequency should be 1! " + allDocsEnum.freq());

        final int docId = allDocsEnum.docID();
        final long docLen = norms.get(docId);

        // handle documents that have zero terms in it. docLen=0 Difference reader.numDocs() - collectionStatistics.docCount()
        if (docLen == 0L) {
            return -1;
        }

        if (docLen < 1) throw new RuntimeException("docLen is less than one!");

//        double relativeFrequency = (double) TF / sumTotalTermFreq;
//        relativeFrequency *= mu;
//        relativeFrequency /= (mu + (double) docLen);

        double relativeFrequency = relativeFrequency(0, docLen, TF);

        if (!(relativeFrequency > 0 && relativeFrequency <= 1))
            throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);

        return binningStrategy.calculateBinValue(relativeFrequency);

    }

    private double relativeFrequency(int tf, long docLen, long TF) {
        final double e_ij = ((double) (TF * docLen)) / ((double) sumTotalTermFreq);
        return (tf + e_ij) / (docLen + e_ij);
    }
}
