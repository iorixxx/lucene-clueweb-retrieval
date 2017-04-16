package edu.anadolu.freq;


import edu.anadolu.analysis.Analyzers;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.anadolu.Searcher.extractRelativeFreq;
import static edu.anadolu.Searcher.extractTerm;

/**
 * Traverse posting list of given term over the field.
 */
public class TermFreqDistribution {

    protected final IndexReader reader;
    protected final BinningStrategy binningStrategy;
    protected final NumericDocValues norms;
    protected final String field;

    public TermFreqDistribution(IndexReader reader, BinningStrategy binningStrategy, String field) throws IOException {
        this.reader = reader;
        this.binningStrategy = binningStrategy;
        this.norms = MultiDocValues.getNormValues(reader, field);
        this.field = field;
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
            final long numTerms = norms.get(postingsEnum.docID());
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
    protected static String rollCountArray(int max, final int[] array) {
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
     * @throws IOException
     */
    public void saveQRelDistribution(String word, String field, Track track, InfoNeed need, final Map<Integer, PrintWriter> writerMap) throws IOException {

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

            final long numTerms = norms.get(postingsEnum.docID());

            writerMap.get(judge).println(docID + "=" + freq + "/" + numTerms);
        }
    }


    public String generateFileName(int judge) {
        return field + "_" + judge + "_freq.csv";
    }

    public void saveQRelDistributionStat(Path base, Track track) throws IOException {

        final Map<Integer, PrintWriter> writerMap = new HashMap<>(10);
        for (int judge : track.getJudgeLevels())
            writerMap.put(judge, new PrintWriter(Files.newBufferedWriter(base.resolve(generateFileName(judge)), StandardCharsets.US_ASCII)));

        for (InfoNeed need : track.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query());

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

    public void saveDistributionStats(Path base, Track track) throws IOException {

        final HashMap<String, String> cache = new HashMap<>(150);
        PrintWriter allOutput = new PrintWriter(Files.newBufferedWriter(base.resolve(outputFileName()), StandardCharsets.US_ASCII));

        for (InfoNeed need : track.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query());

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

        }

        cache.clear();
        allOutput.flush();
        allOutput.close();
    }

    public void processSingeTrack(Track track, Path path) throws IOException {
        System.out.println(field + "=" + track.toString() + track.getJudgeLevels());
        saveDistributionStats(path, track);
    }

    public static void mainForStopWords(final BinningStrategy binningStrategy, final String field, Path indexPath, final Path freqsPath, final Set<String> stopWords) throws IOException {

        final IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        final String indexTag = indexPath.getFileName().toString();
        final TermFreqDistribution distribution = new TermFreqDistribution(reader, binningStrategy, field);
        final Path path = Paths.get(freqsPath.toString(), indexTag, "StopWords");
        if (!Files.exists(path)) Files.createDirectories(path);

        final PrintWriter stopOutput = new PrintWriter(Files.newBufferedWriter(path.resolve(field + "_stop_freq.csv"), StandardCharsets.US_ASCII));

        for (final String stopWord : stopWords) {
            stopOutput.println(Analyzers.getAnalyzedToken(stopWord) + "\t" + distribution.getTermDistributionStats(Analyzers.getAnalyzedToken(stopWord)));
        }

        stopOutput.flush();
        stopOutput.close();


        reader.close();

    }

    public static void mainWithThreads(final BinningStrategy binningStrategy, final Track[] tracks, final String field, Path indexPath, final Path freqsPath, int numThreads) throws IOException, InterruptedException {

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        System.out.println("Good Old Freq.Dist opened index directory : " + indexPath + " has " + reader.numDocs() + " numDocs and has " + reader.maxDoc() + " maxDocs");

        TermFreqDistribution distribution = new TermFreqDistribution(reader, binningStrategy, field);
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

    /**
     * Save TermFreqDist over result lists returned by different term-weighting models.
     * Save aligned query relevance judgement levels too.
     */
    public static void saveTermFreqDistOverResultList(Path indexPath, Track[] tracks, String home, Collection<ModelBase> models) throws IOException, ParseException {

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));

        final String indexTag = indexPath.getFileName().toString();

        for (ModelBase model : models) {

            if ("DefaultSimilarity".equals(model.toString())) continue;

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(model);

            final Path model_path = Paths.get(home, "freqs", indexTag, "models");
            if (!Files.exists(model_path))
                Files.createDirectories(model_path);

            PrintWriter model_out = new PrintWriter(Files.newBufferedWriter(
                    model_path.resolve(model.toString() + ".freq"),
                    StandardCharsets.US_ASCII));

            PrintWriter label_out = new PrintWriter(Files.newBufferedWriter(
                    model_path.resolve(model.toString() + ".label"),
                    StandardCharsets.US_ASCII));

            QueryParser queryParser = new QueryParser("contents", Analyzers.analyzer());
            queryParser.setDefaultOperator(QueryParser.Operator.AND);

            for (Track track : tracks)
                for (InfoNeed need : track.getTopics()) {

                    Map<String, StringBuilder> bufferMap = new LinkedHashMap<>();

                    for (String s : Analyzers.getAnalyzedTokens(need.query()))
                        bufferMap.put(s, new StringBuilder());

                    Query query = queryParser.parse(need.query());

                    ScoreDoc[] hits;
                    try {
                        hits = searcher.search(query, 1000).scoreDocs;
                    } catch (java.lang.AssertionError ae) {
                        System.out.println("indexTag: " + indexTag + " similarity: " + model + " " + need.toString());
                        ae.printStackTrace();
                        continue;
                    }

                    label_out.print(need.id() + "\t");

                    for (ScoreDoc scoreDoc : hits) {
                        int docId = scoreDoc.doc;

                        Document doc = searcher.doc(docId);
                        String docID = doc.get("id");
                        int j = need.getJudge(docID);
                        label_out.print(j + "\t");

                        Explanation explanation = searcher.explain(query, docId);
                        Explanation[] first = explanation.getDetails();

                        if (first.length != need.wordCount())
                            throw new RuntimeException("explanation array size mismatch :" + need.wordCount() + " exp:" + first.length);

                        if (need.wordCount() == 1) {

                            try {
                                String word = extractTerm(explanation);
                                double relativeFreq = extractRelativeFreq(explanation.getDetails()[0]);
                                //TODO if termCount != wordCount there is a bug: double counting, relativeFreq > 1
                                bufferMap.get(word).append(String.format("%.6f", relativeFreq)).append("\t");
                            } catch (RuntimeException r) {
                                System.out.println("indexTag: " + indexTag + " similarity: " + model + " " + need.toString());
                                System.out.println(explanation.toString());
                                r.printStackTrace();
                            }

                        } else {

                            for (Explanation exp : first)
                                try {
                                    String word = extractTerm(exp);
                                    double relativeFreq = extractRelativeFreq(exp.getDetails()[0]);
                                    bufferMap.get(word).append(String.format("%.6f", relativeFreq)).append("\t");

                                } catch (RuntimeException r) {
                                    System.out.println("indexTag: " + indexTag + " similarity: " + model + " " + need.toString());
                                    System.out.println(explanation.toString());
                                    r.printStackTrace();
                                }
                        }

                    }

                    label_out.println();
                    for (String s : Analyzers.getAnalyzedTokens(need.query())) {
                        model_out.println(need.id() + ":" + s + "\t" + bufferMap.get(s).toString().trim());
                    }
                }


            model_out.flush();
            model_out.close();
            label_out.flush();
            label_out.close();
        }

        reader.close();
    }

}
