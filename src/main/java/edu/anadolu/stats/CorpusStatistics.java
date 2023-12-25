package edu.anadolu.stats;

import org.apache.commons.math3.util.Precision;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;

/**
 * ClueWeb09 Collection Statistic
 */
public final class CorpusStatistics implements Closeable {

    private final Path termsPath;
    private final IndexReader reader;
    private final IndexSearcher searcher;

    public CorpusStatistics(Path indexPath, Path statsPath) throws IOException {

        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        if (!Files.exists(statsPath))
            Files.createDirectories(statsPath);

        termsPath = statsPath.resolve(indexPath.getFileName().toString());

        if (!Files.exists(termsPath))
            Files.createDirectories(termsPath);

        reader = DirectoryReader.open(FSDirectory.open(indexPath));
        searcher = new IndexSearcher(reader);

        System.out.println("Opened index directory : " + indexPath + " has " + reader.numDocs() + " numDocs and has " + reader.maxDoc() + " maxDocs");

    }

    public void saveTermStatsForWords(String field, Set<String> set, String fileName) throws IOException {

        final NumericDocValues norms = MultiDocValues.getNormValues(reader, field);

        final long docCount = searcher.collectionStatistics(field).docCount();
        final long sumTotalTermFreq = searcher.collectionStatistics(field).sumTotalTermFreq();

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(termsPath.resolve(fileName)));

        out.println("term \t totalTermFreq \t docFreq \t cti");

        for (String word : set) {
            Term term = new Term(field, word);
            TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));

            final long termFrequency = termStatistics.totalTermFreq();
            final long df = termStatistics.docFreq();

            PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

            double cti = 0.0;
            if (postingsEnum != null)
                while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

                    final int tf = postingsEnum.freq();

                    final long docLen;

                    if (norms.advanceExact(postingsEnum.docID())) {
                        docLen = norms.longValue();
                    } else {
                        docLen = 0;
                    }

                    final double e_ij = (double) (termFrequency * docLen) / (double) sumTotalTermFreq;

                    cti += Math.pow((tf - e_ij), 2) / e_ij;

                }

            long remainingDocs = docCount - df;
            double e = (double) termFrequency / docCount;

            cti += remainingDocs * e;

            cti /= docCount;

            out.println(word + "\t" + termFrequency + "\t" + df + "\t" + String.format("%.5f", cti));

        }

        out.flush();
        out.close();
    }

    public String fieldHeader() {
        return "field \t sumTotalTermFreq \t docCount \t avgLength";
    }

    private void dumpCollectionStats(CollectionStatistics collectionStatistics, PrintWriter out) {
        out.print(NumberFormat.getNumberInstance(Locale.US).format(collectionStatistics.sumTotalTermFreq()));
        out.print(" & ");
        out.print(NumberFormat.getNumberInstance(Locale.US).format(collectionStatistics.docCount()));
        out.print(" & ");
        out.print(Precision.round((double) collectionStatistics.sumTotalTermFreq() / collectionStatistics.docCount(), 1));
        out.print("\\\\");
        out.println();
    }

    /**
     * Save corpus statistics to a LaTex file
     *
     * @param a document representations
     * @throws IOException
     */
    public void saveLaTexStats(String[] a) throws IOException {

        List<String> fields = new ArrayList<>(Arrays.asList(a));

        fields.remove("contents");

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(termsPath.resolve("field_stats.tex"), StandardCharsets.US_ASCII));

        out.println("\\bfseries  Representation &  \\bfseries Tokens &  \\bfseries \\# of Documents & \\bfseries Avg. Length  \\\\");

        out.println("\\hline");
        out.print("Entire Document & ");
        dumpCollectionStats(searcher.collectionStatistics("contents"), out);

        out.println("\\hline");

        for (String field : fields) {

            out.print(field);
            out.print(" & ");
            dumpCollectionStats(searcher.collectionStatistics(field), out);

        }

        out.println("\\hline");

        out.flush();
        out.close();

    }


    /**
     * Save corpus statistics to comma separated values (CSV)
     *
     * @param fields document representations
     * @throws IOException
     */
    public void saveFieldStats(String[] fields) throws IOException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(termsPath.resolve("field_stats.csv"), StandardCharsets.US_ASCII));

        out.println(fieldHeader());

        for (String field : fields)
            out.println(field + "\t" + fieldStats(field, "\t"));

        out.flush();
        out.close();
    }

    public String fieldStats(String field, String separator) throws IOException {

        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);

        //   System.out.println(field + "\t sumTotalTermFreq \t " + collectionStatistics.sumTotalTermFreq());
        //   System.out.println(field + "\t docCount \t " + collectionStatistics.docCount());

        return collectionStatistics.sumTotalTermFreq() + separator + collectionStatistics.docCount() + separator + Precision.round((double) collectionStatistics.sumTotalTermFreq() / collectionStatistics.docCount(), 1);

    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
