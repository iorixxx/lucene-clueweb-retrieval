package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.Similarity;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.Indexer.FIELD_CONTENTS;
import static edu.anadolu.Indexer.FIELD_ID;

/**
 * Modified version of Searcher. Iterates over the result list and computes term-weighting scores for learning to rank.
 */
public class FeatureSearcher extends Searcher {

    private NumericDocValues norms;

    public FeatureSearcher(Path indexPath, DataSet dataSet, int numHits) throws IOException {
        super(indexPath, dataSet, numHits);

    }

    public void search(Track track, Similarity similarity, QueryParser.Operator operator, String field, Path path, Collection<ModelBase> models) throws IOException, ParseException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        final long docCount = collectionStatistics.docCount();
        final long sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();

        this.norms = MultiDocValues.getNormValues(reader, field);

        final String runTag = toString(similarity, operator, field, 0);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve(runTag + ".features"), StandardCharsets.US_ASCII));


        QueryParser queryParser = new QueryParser(field, Analyzers.analyzer(analyzerTag));
        queryParser.setDefaultOperator(operator);

        Map<String, TermStatistics> termStatisticsMap = new HashMap<>();

        for (InfoNeed need : track.getTopics()) {

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query(), Analyzers.analyzer(analyzerTag));

            for (String word : subParts) {
                if (termStatisticsMap.containsKey(word)) continue;
                Term term = new Term(field, word);
                TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
                termStatisticsMap.put(word, termStatistics);
            }

            String queryString = need.query();
            Query query = queryParser.parse(queryString);

            ScoreDoc[] hits = searcher.search(query, numHits).scoreDocs;

            if (hits.length == 0) {
                //out.print(need.id()); out.print(dataSet.getNoDocumentsID());
                continue;
            }

            LinkedHashMap<Integer, List<DocTermStat>> map = new LinkedHashMap<>(numHits);

            for (ScoreDoc hit : hits) {
                map.put(hit.doc, new ArrayList<>(subParts.size()));
            }

            for (String word : subParts)
                findDoc(map, word, field);


            for (Map.Entry<Integer, List<DocTermStat>> entry : map.entrySet()) {

                Document doc = searcher.doc(entry.getKey());

                final int judge = need.getJudgeMap().getOrDefault(doc.get(FIELD_ID), 0);

                out.print(Integer.toString(judge == -2 ? 0 : judge));
                out.print(" ");

                out.print("qid:");
                out.print(need.id());
                out.print(" ");

                out.print("1:");
                out.print(need.wordCount());
                out.print(" ");

                out.print("2:");
                out.print(need.termCount());
                out.print(" ");

                int sum = 0;
                for (DocTermStat docTermStat : entry.getValue()) {
                    if (-1 == docTermStat.tf) continue;
                    sum += docTermStat.tf;
                }

                out.print("3:");
                out.print(sum);
                out.print(" ");

                int f = 3;

                for (ModelBase m : models) {

                    double score = 0.0;


                    for (DocTermStat docTermStat : entry.getValue()) {

                        if (-1 == docTermStat.tf) continue;

                        TermStatistics termStatistics = termStatisticsMap.get(docTermStat.word);

                        score += m.score(docTermStat.tf, docTermStat.dl, (double) sumTotalTermFreq / docCount, 1, termStatistics.docFreq(), termStatistics.totalTermFreq(), docCount, sumTotalTermFreq);
                    }

                    out.print(Integer.toString(++f));
                    out.print(":");
                    out.print(String.format("%.5f", score));
                    out.print(" ");

                    //  if (m.toString().equals(similarity.toString())) {
                    //     System.out.println(String.format("%.5f %.5f", hit.score, score));
                    // }

                }

                out.print("# ");
                out.print(doc.get(FIELD_ID));
                out.println();
                out.flush();
            }

            subParts.clear();
            map.clear();
        }

        termStatisticsMap.clear();
        out.flush();
        out.close();
    }

    class DocTermStat {

        private final long dl;
        private final int tf;
        private final String word;

        DocTermStat(String word, long dl, int tf) {
            this.dl = dl;
            this.tf = tf;
            this.word = word;
        }
    }

    private DocTermStat findDoc(int docId, String word, String field) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the word " + word + " in the field " + field);
            return new DocTermStat(word, -1, -1);
        }

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            if (postingsEnum.docID() == docId) {
                if (norms.advanceExact(postingsEnum.docID())) {
                    return new DocTermStat(word, norms.longValue(), postingsEnum.freq());
                } else {
                    throw new RuntimeException("norms.advanceExact() cannot find " + postingsEnum.docID());
                }
            }
        }

        //System.out.println("Cannot find docId " + docId);
        return new DocTermStat(word, -1, -1);
    }

    private void findDoc(LinkedHashMap<Integer, List<DocTermStat>> map, String word, String field) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the word " + word + " in the field " + field);
            for (Integer i : map.keySet())
                map.get(i).add(new DocTermStat(word, -1, -1));
            return;
        }

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            if (map.containsKey(postingsEnum.docID())) {
                if (norms.advanceExact(postingsEnum.docID())) {
                    map.get(postingsEnum.docID()).add(new DocTermStat(word, norms.longValue(), postingsEnum.freq()));
                } else {
                    throw new RuntimeException("norms.advanceExact() cannot find " + postingsEnum.docID());
                }
            }
        }
    }


    public void searchF(Collection<ModelBase> models, String runsPath) throws IOException {

        System.out.println("There are " + models.size() * dataSet.tracks().length + " many tasks to process...");

        for (final Track track : dataSet.tracks()) {
            final Path path = Paths.get(dataSet.collectionPath().toString(), runsPath, indexTag, track.toString());
            createDirectories(path);
        }

        for (final Track track : dataSet.tracks()) {

            models.parallelStream().forEach(model -> {
                        try {
                            search(track, model, QueryParser.Operator.OR, FIELD_CONTENTS, Paths.get(dataSet.collectionPath().toString(), runsPath, indexTag, track.toString()), models);
                        } catch (ParseException | IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }
}
