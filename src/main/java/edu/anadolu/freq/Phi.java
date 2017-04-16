package edu.anadolu.freq;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Calculates term frequency distribution from Phi
 */
public final class Phi extends TermFreqDistribution {

    private final NormalDistribution normalDistribution = new NormalDistribution();
    private final IndexSearcher searcher;
    private final long sumTotalTermFreq;

    public Phi(IndexReader reader, BinningStrategy binningStrategy, String field) throws IOException {
        super(reader, binningStrategy, field);
        this.searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();
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
            final long numTerms = norms.get(postingsEnum.docID());

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

    public static void mainWithThreads(final BinningStrategy binningStrategy, final Track[] tracks, final String field, Path indexPath, final Path freqsPath, int numThreads) throws IOException, InterruptedException {

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        System.out.println("Phi Freq.Dist opened index directory : " + indexPath + " has " + reader.numDocs() + " numDocs and has " + reader.maxDoc() + " maxDocs");

        Phi distribution = new Phi(reader, binningStrategy, field);
        final String indexTag = indexPath.getFileName().toString();

        for (final Track track : tracks) {

            final Path path = Paths.get(freqsPath.toString(), indexTag, track.toString());
            if (!Files.exists(path))
                Files.createDirectories(path);

            executor.execute(new Thread(indexTag + track.toString()) {
                @Override
                public void run() {
                    try {
                        distribution.processSingeTrack(track, path);
                    } catch (IOException ioe) {
                        System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                        ioe.printStackTrace();
                    }

                }
            });
        }


        //add some delay to let some threads spawn by scheduler
        Thread.sleep(30000);
        executor.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait for existing tasks to terminate
            while (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        reader.close();
    }
}
