package edu.anadolu.freq;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


/**
 * Freq. Dist. implementation that handles a query as a whole
 */
public final class QueryFreqDistribution extends TermFreqDistribution {

    private final IndexSearcher searcher;
    private final Path freqsPath;

    public QueryFreqDistribution(IndexReader reader, Path freqsPath, BinningStrategy binningStrategy, String field) throws IOException {
        super(reader, binningStrategy, field);
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(new MetaTerm());
        this.freqsPath = freqsPath;

        if (!Files.exists(freqsPath))
            Files.createDirectories(freqsPath);
    }

    public void process(List<InfoNeed> needs, String indexTag, int numHits) throws ParseException, IOException {

        QueryParser queryParser = new QueryParser(field, Analyzers.analyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(Paths.get(freqsPath.toString(), "QueryFreqDist" + indexTag + ".csv"), StandardCharsets.US_ASCII));

        PrintWriter docLength = new PrintWriter(Files.newBufferedWriter(Paths.get(freqsPath.toString(), "QueryDocLength" + indexTag + ".csv"), StandardCharsets.US_ASCII));

        docLength.println("qID\tN\tdocLenAcc\tdocLenSquareAcc");

        for (InfoNeed need : needs) {

            final int[] array = new int[1001];
            Arrays.fill(array, 0);

            Query query;

            if (need.termCount() == need.wordCount()) {
                query = queryParser.parse(need.query());
            } else {
                System.out.println(need + " unique terms " + need.getDistinctQuery());
                query = queryParser.parse(need.getDistinctQuery());
            }

            ScoreDoc[] hits = searcher.search(query, numHits).scoreDocs;


            int max = 0;
            long docLenAcc = 0;
            long docLenSquareAcc = 0;

            for (ScoreDoc scoreDoc : hits) {
                int docId = scoreDoc.doc;
                long docLen = norms.get(docId);

                double relativeFrequency = scoreDoc.score;

                if (relativeFrequency > 1) {
                    System.out.println(need + " returned MetaTerm score greater than 1 ");
                }

                final int value = binningStrategy.calculateBinValue(relativeFrequency);

                array[value]++;
                if (value > max)
                    max = value;

                docLenAcc += docLen;
                docLenSquareAcc += (docLen * docLen);
            }
            output.println(need.id() + "\t" + rollCountArray(max, array));
            output.flush();
            docLength.println(need.id() + "\t" + hits.length + "\t" + docLenAcc + "\t" + docLenSquareAcc);
            docLength.flush();

        }

        output.close();
        docLength.close();
    }
}
