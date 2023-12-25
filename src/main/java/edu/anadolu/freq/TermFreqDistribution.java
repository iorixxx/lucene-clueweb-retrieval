package edu.anadolu.freq;


import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Traverse posting list of given term over the field.
 */
public class TermFreqDistribution implements TFD {

    protected final IndexReader reader;
    final BinningStrategy binningStrategy;
    protected final NumericDocValues norms;
    protected final String field;

    protected final Analyzer analyzer;

    public TermFreqDistribution(IndexReader reader, BinningStrategy binningStrategy, String field, Analyzer analyzer) throws IOException {
        this.reader = reader;
        this.binningStrategy = binningStrategy;
        this.norms = MultiDocValues.getNormValues(reader, field);
        this.field = field;
        this.analyzer = analyzer;
    }

    public String getTermDistributionStats(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) return word + "(stopword)";

        final int[] array = new int[binningStrategy.numBins() + 1];
        Arrays.fill(array, 0);

        int max = 0;
        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final int freq = postingsEnum.freq();

            final long numTerms;

            if (norms.advanceExact(postingsEnum.docID())) {
                numTerms = norms.longValue();
            } else {
                numTerms = 0;
            }

            final double relativeFrequency = (double) freq / (double) numTerms;

            if (!(relativeFrequency > 0 && relativeFrequency <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + relativeFrequency);


            final int value = binningStrategy.calculateBinValue(relativeFrequency);

            array[value]++;
            if (value > max)
                max = value;

        }

        return rollCountArray(max, array);
    }

    /**
     * Convert count array to String
     * Fill count array with zeros
     *
     * @param max maximum length
     * @return String
     */
    static String rollCountArray(int max, final int[] array) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 1; i <= max; i++) {
            buffer.append(array[i]);
            buffer.append("\t");
        }
        return buffer.toString().trim();
    }


    /**
     * Frequency Distribution over Query Relevance Judgments
     *
     * @param word  singe query term
     * @param field field
     * @param track track
     * @param need  topic
     * @throws IOException if occurs during file write operations
     */
    private void saveQRelDistribution(String word, String field, Track track, InfoNeed need, final Map<Integer, PrintWriter> writerMap) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            for (int key : track.getJudgeLevels())
                writerMap.get(key).println(word + "(stopword)");
            return;
        }

        for (int key : track.getJudgeLevels()) {
            if (writerMap.containsKey(key))
                writerMap.get(key).println(Integer.toString(need.id()) + ":" + word);
            else
                throw new RuntimeException("writerMap does not contain the key :" + key + " for the field :" + field);
        }

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            String docID = reader.document(postingsEnum.docID()).get("id");

            final int judge = need.getJudge(docID);

            if (judge == -5) continue;

            final int freq = postingsEnum.freq();

            final long numTerms;

            if (norms.advanceExact(postingsEnum.docID())) {
                numTerms = norms.longValue();
            } else {
                numTerms = 0;
            }

            writerMap.get(judge).println(docID + "=" + freq + "/" + numTerms);
        }
    }


    private String generateFileName(int judge) {
        return field + "_" + judge + "_freq.csv";
    }

    public void saveQRelDistributionStat(Path base, Track track) throws IOException {

        final Map<Integer, PrintWriter> writerMap = new HashMap<>(10);
        for (int judge : track.getJudgeLevels())
            writerMap.put(judge, new PrintWriter(Files.newBufferedWriter(base.resolve(generateFileName(judge)))));

        for (InfoNeed need : track.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query(), analyzer);

            // Topics 95 and 100 from WT10 do not have relevance judgments.
            if (!track.isJudged(need.id())) {
                System.err.println("Skipping topic " + need.id() + " from " + track.toString() + ". It is not judged at all.");
                continue;
            }

            for (String word : subParts)
                saveQRelDistribution(word, field, track, need, writerMap);

        }

        for (Map.Entry<Integer, PrintWriter> entry : writerMap.entrySet()) {
            PrintWriter output = entry.getValue();
            output.flush();
            output.close();
        }
    }

    protected String outputFileName() {
        return field + "_all_freq_" + binningStrategy.numBins() + ".csv";
    }

    private void saveDistributionStats(Path base, Track track) throws IOException {

        final HashMap<String, String> cache = new HashMap<>(150);
        PrintWriter allOutput = new PrintWriter(Files.newBufferedWriter(base.resolve(outputFileName())));

        for (InfoNeed need : track.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query(), analyzer);

            for (String word : subParts) {
                final String line;
                if (cache.containsKey(word))
                    line = cache.get(word);
                else {
                    line = getTermDistributionStats(word);
                    cache.put(word, line);
                }
                allOutput.println(need.id() + ":" + word + "\t" + line);
            }
            subParts.clear();
        }

        cache.clear();
        allOutput.flush();
        allOutput.close();
    }

    @Override
    public void processSingeTrack(Track track, Path path) throws IOException {
        System.out.println(field + "=" + track.toString() + track.getJudgeLevels());
        saveDistributionStats(path, track);
    }

    @Override
    public String toString() {
        return "Good Old Term Freq. Distribution";
    }


    public static void mainForStopWords(final BinningStrategy binningStrategy, final String field, Path indexPath, final Path freqsPath, final Set<String> stopWords) throws IOException {

        final IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        final String indexTag = indexPath.getFileName().toString();
        Tag tag = Tag.tag(indexTag);
        System.out.println("analyzer tag " + tag);
        Analyzer analyzer = new WhitespaceAnalyzer(); //Analyzers.analyzer(tag);
        final TermFreqDistribution distribution = new TermFreqDistribution(reader, binningStrategy, field, analyzer);
        final Path path = Paths.get(freqsPath.toString(), indexTag, "StopWords");
        if (!Files.exists(path)) Files.createDirectories(path);

        final PrintWriter stopOutput = new PrintWriter(Files.newBufferedWriter(path.resolve(field + "_stop_freq.csv")));

        for (final String stopWord : stopWords) {
            stopOutput.println(Analyzers.getAnalyzedToken(stopWord, analyzer) + "\t" + distribution.getTermDistributionStats(Analyzers.getAnalyzedToken(stopWord, analyzer)));
        }

        stopOutput.flush();
        stopOutput.close();
        reader.close();

    }
}
