package edu.anadolu.freq;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.commons.math3.util.Precision;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Traverse posting list of given term over the contents field.
 * Dumps observed term frequencies in a verbose manner.
 * Output of this dumper is meant to fed to MATLAB.
 */
public final class VerboseTFDumper implements Closeable {

    private final double D_BAR;

    private final IndexReader reader;
    private final Path verbosePath;
    private final Path otqPath;
    private final int[] array = new int[780];

    private final Set<String> distinctTerms;

    public double getD_BAR() {
        return this.D_BAR;
    }

    private final Analyzer analyzer;

    public VerboseTFDumper(Path collectionPath, Path indexPath, Set<String> distinctTerms) throws IOException {

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        this.analyzer = Analyzers.analyzer(Tag.tag(indexPath.getFileName().toString()));

        IndexSearcher searcher = new IndexSearcher(this.reader);
        final long docCount = searcher.collectionStatistics("contents").docCount();
        final long sumTotalTermFreq = searcher.collectionStatistics("contents").sumTotalTermFreq();


        this.verbosePath = Paths.get(collectionPath.toString(), "verbose_freqs", indexPath.getFileName().toString());

        this.otqPath = Paths.get(collectionPath.toString(), "verbose_freqs", indexPath.getFileName().toString(), "otq");

        if (!Files.exists(verbosePath))
            Files.createDirectories(verbosePath);

        if (!Files.exists(otqPath))
            Files.createDirectories(otqPath);

        this.D_BAR = (double) sumTotalTermFreq / docCount;

        this.distinctTerms = distinctTerms;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private long getNumTerms(int docID, String field) throws IOException {

        IndexableField f = reader.document(docID).getField(field + "Length");

        if (f != null)
            return Long.parseLong(f.stringValue());
        else
            throw new IllegalStateException("field : " + field + " does not have length indexed!");

    }

    public void saveTermFrequenciesForAllQueries(String field) throws IOException {
        for (String word : distinctTerms)
            saveTermFrequencies(word, field);
    }

    public void saveTermFrequenciesForOneTermQueries(String field, Track[] tracks) throws IOException {

        for (Track track : tracks)
            for (InfoNeed need : track.getTopics()) {

                if (need.wordCount() != 1) continue;

                // QRels of a topic, docID, judgeLevel pairs
                Map<String, Integer> judgeMap = need.getJudgeMap();

                String word = Analyzers.getAnalyzedToken(need.query(), analyzer);

                saveTermFrequencies(word, field, judgeMap);

            }
    }

    public void saveNormalizedTermFrequenciesForAllQueries(String field, TFNormalization normalization) throws IOException {
        for (String word : distinctTerms)
            saveNormalizedTermFrequencies(word, field, normalization);
    }

    public void saveNormalizedTermFrequencies(String word, String field, TFNormalization normalization) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.err.println("Word " + word + " looks like a stop word");
            return;
        }

        Path path = verbosePath.resolve(normalization.toString());
        if (!Files.exists(path))
            Files.createDirectories(path);

        PrintWriter output = new PrintWriter(
                Files.newBufferedWriter(path.resolve(word), StandardCharsets.US_ASCII)

        );

        TreeMap<Double, Integer> treeMap = new TreeMap<>();

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final double tfn = normalization.tfn(postingsEnum.freq(), getNumTerms(postingsEnum.docID(), field));

            final double key = Precision.round(tfn, 4);

            // System.out.println(tfn + " => " + key);

            if (treeMap.containsKey(key)) {
                int count = treeMap.get(key);
                treeMap.put(key, count + 1);
            } else treeMap.put(key, 1);


        }

        output.println("value\tcount");
        for (Map.Entry<Double, Integer> entry : treeMap.entrySet())
            output.println(entry.getKey() + "\t" + entry.getValue());

        output.flush();
        output.close();
        treeMap.clear();


    }

    public void saveTermFrequencies(String word, String field) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.err.println("Word " + word + " looks like a stop word");
            return;
        }

        PrintWriter output = new PrintWriter(
                Files.newBufferedWriter(verbosePath.resolve(word), StandardCharsets.US_ASCII)

        );


        Arrays.fill(array, 0);

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final double percentage = (double) postingsEnum.freq() / getNumTerms(postingsEnum.docID(), field);

            if (!(percentage > 0 && percentage <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + percentage);

            long value = ceil(percentage * D_BAR);

            if (value < 1)
                System.err.println("value is less than one : " + value);

            array[(int) value]++;

        }

        output.println("value\tcount");
        for (int i = 0; i < array.length; i++)
            if (array[i] != 0)
                output.println(i + "\t" + array[i]);


        output.flush();
        output.close();


    }

    public void saveTermFrequencies(String word, String field, Map<String, Integer> judgeMap) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.err.println("Word " + word + " looks like a stop word");
            return;
        }

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(otqPath.resolve(word), StandardCharsets.US_ASCII));


        int[] relArray = new int[780];
        int[] nonRelArray = new int[780];
        Arrays.fill(relArray, 0);
        Arrays.fill(nonRelArray, 0);

        Arrays.fill(array, 0);

        Map<Integer, List<Integer>> judgeList = new HashMap<>();

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final double percentage = (double) postingsEnum.freq() / getNumTerms(postingsEnum.docID(), field);

            if (!(percentage > 0 && percentage <= 1))
                throw new RuntimeException("percentage is out of range exception, percentage = " + percentage);

            long value = ceil(percentage * D_BAR);

            if (value < 1)
                System.err.println("value is less than one : " + value);

            array[(int) value]++;

            int intValue = (int) value;

            String docID = reader.document(postingsEnum.docID()).getField("id").stringValue();

            if (!judgeMap.containsKey(docID)) continue;

            int judge = judgeMap.get(docID);

            if (judge > 0)
                relArray[intValue]++;
            else
                nonRelArray[intValue]++;

            if (judgeList.containsKey(intValue))
                judgeList.get(intValue).add(judge);
            else {
                List<Integer> integerList = new ArrayList<>();
                integerList.add(judge);
                judgeList.put(intValue, integerList);
            }

        }

        output.println("value\tcount\trelevant\tnonRelevant\tlevels");
        for (int i = 0; i < array.length; i++)
            if (array[i] != 0) {
                output.print(i + "\t" + array[i]);

                if (relArray[i] > 0 && nonRelArray[i] > 0)
                    output.print("\t" + relArray[i] + "\t" + nonRelArray[i]);

                if (judgeList.containsKey(i)) {
                    List<Integer> list = judgeList.get(i);
                    Collections.sort(list);
                    Collections.reverse(list);
                    output.print("\t");
                    output.print(list);
                }
                output.println();
            }


        output.flush();
        output.close();


    }

    private static long ceil(final double d) {
        return Math.round(d + .5);
    }
}
