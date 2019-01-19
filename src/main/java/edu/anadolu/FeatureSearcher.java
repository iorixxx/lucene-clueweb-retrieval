package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.Similarity;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.Indexer.FIELD_CONTENTS;
import static edu.anadolu.Indexer.FIELD_ID;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Modified version of Searcher. Iterates over the result list and computes term-weighting scores (type Q-D features) for learning to rank.
 */
public class FeatureSearcher extends Searcher {

    static private boolean floatIsDifferent(float f1, float f2, float delta) {
        if (Float.compare(f1, f2) == 0) {
            return false;
        }
        if ((Math.abs(f1 - f2) <= delta)) {
            return false;
        }

        return true;
    }

    public FeatureSearcher(Path indexPath, DataSet dataSet, int numHits) throws IOException {
        super(indexPath, dataSet, numHits);
    }

    public void search(Track track, Similarity similarity, QueryParser.Operator operator, String field, Path path, Collection<ModelBase> models) throws IOException, ParseException {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        final long docCount = collectionStatistics.docCount();
        final long sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();

        final List<LeafReaderContext> leaves = reader.leaves();
        if (leaves.size() == 1) {
            NumericDocValues norms = leaves.get(0).reader().getNormValues(field);
        }

        NumericDocValues norms = MultiDocValues.getNormValues(reader, field);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve(Evaluator.prettyModel(similarity.toString()) + ".features"), StandardCharsets.US_ASCII));

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

            float[] scores = new float[hits.length];

            if (hits.length == 0) {
                //out.print(need.id()); out.print(dataSet.getNoDocumentsID());
                continue;
            }

            LinkedHashMap<Integer, List<DocTermStat>> map = new LinkedHashMap<>(numHits);

            int i = 0;
            for (ScoreDoc hit : hits) {
                map.put(hit.doc, new ArrayList<>(subParts.size()));
                scores[i++] = hit.score;
            }

            for (String word : subParts)
                findDoc(map, word, field, norms);

            i = 0;
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

                    if (m.toString().equals(similarity.toString())) {
                        float s = scores[i++];
                        if (floatIsDifferent((float) score, s, 0.001f))
                            System.out.println(String.format("%S %s %d %.5f %.5f", need.toString(), m.toString(), i, s, score));
                    }
                }

                // document length
                if (norms.advanceExact(entry.getKey())) {
                    out.print(Integer.toString(++f));
                    out.print(":");
                    out.print(norms.longValue());
                    out.print(" ");
                } else {
                    throw new RuntimeException("cannot find docId" + entry.getKey());
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

    private long localDocLen(int luceneId, String field) throws IOException {

        NumericDocValues norms = MultiDocValues.getNormValues(reader, field);

        if (norms.advanceExact(luceneId)) {
            return norms.longValue();
        } else {
            throw new RuntimeException("norms.advanceExact() cannot find " + luceneId);
        }
    }

    private long localDocLen2(int luceneId, String field) throws IOException {

        final List<LeafReaderContext> leaves = reader.leaves();
        if (leaves.size() == 1) {
            NumericDocValues norms = leaves.get(0).reader().getNormValues(field);
            if (norms.advanceExact(luceneId)) {
                return norms.longValue();
            } else {
                throw new RuntimeException("norms.advanceExact() cannot find " + luceneId);
            }
        } else
            throw new RuntimeException("leaves.size() != 1");
    }

    private void findDoc(LinkedHashMap<Integer, List<DocTermStat>> map, String word, String field, NumericDocValues norms) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the word " + word + " in the field " + field);
            for (Integer i : map.keySet())
                map.get(i).add(new DocTermStat(word, -1, -1));
            return;
        }

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

            final int luceneId = postingsEnum.docID();

            if (!map.containsKey(luceneId)) continue;

            List<DocTermStat> list = map.get(luceneId);

            if (norms.advanceExact(luceneId)) {

                long dl = norms.longValue();

                if (!list.isEmpty())
                    if (list.get(0).dl != dl)
                        throw new RuntimeException("list.get(0).dl is different from current dl");

                list.add(new DocTermStat(word, dl, postingsEnum.freq()));

                if (dl != localDocLen(luceneId, field))
                    throw new RuntimeException("dl is different from localDocLen");

                if (dl != localDocLen2(luceneId, field))
                    throw new RuntimeException("dl is different from localDocLen");

            } else {
                throw new RuntimeException("norms.advanceExact() cannot find " + luceneId);
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

    /**
     * @param models 8 term-weighting models
     * @param fields list of fields e.g., title, anchor, body
     */
    public void searchF(Collection<ModelBase> models, Collection<String> fields) {

        System.out.println("There are " + models.size() * dataSet.tracks().length * fields.size() + " many tasks to process...");

        for (final Track track : dataSet.tracks())
            for (final String field : fields) {
                models.stream().parallel().forEach(model -> {
                            try {
                                search(track, model, field, Paths.get(dataSet.collectionPath().toString(), "features", "KStem", track.toString()), models);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
    }

    /**
     * A version of findDoc {@link FeatureSearcher#findDoc(java.util.LinkedHashMap, java.lang.String, java.lang.String, org.apache.lucene.index.NumericDocValues)}
     * where keys of the map are ClueWeb09 document identifiers.
     */
    private void findDoc(LinkedHashMap<String, List<DocTermStat>> map, String word, String field, IndexSearcher searcher) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the word " + word + " in the field " + field);
            for (String s : map.keySet())
                map.get(s).add(new DocTermStat(word, -1, -1));
            return;
        }

        while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

            final int luceneId = postingsEnum.docID();

            String docId = searcher.doc(luceneId).get(FIELD_ID);

            if (map.containsKey(docId)) {

                List<DocTermStat> list = map.get(docId);

                if (list.isEmpty()) {
                    NumericDocValues norms = MultiDocValues.getNormValues(reader, field);
                    if (norms.advanceExact(luceneId)) {
                        list.add(new DocTermStat(word, norms.longValue(), postingsEnum.freq()));
                    } else {
                        throw new RuntimeException("norms.advanceExact() cannot find " + luceneId);
                    }
                } else
                    list.add(new DocTermStat(word, list.get(0).dl, postingsEnum.freq()));
            }
        }
    }

    public void search(Track track, Similarity similarity, String field, Path path, Collection<ModelBase> models) throws IOException {

        IndexSearcher searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        final long docCount = collectionStatistics.docCount();
        final long sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();
        LinkedHashMap<Integer, ArrayList<String>> resultList = new LinkedHashMap<>(numHits);

        try (BufferedReader reader = Files.newBufferedReader(path.resolve(Evaluator.prettyModel(similarity.toString()) + ".features"), StandardCharsets.US_ASCII)) {

            for (; ; ) {

                String line = reader.readLine();
                if (line == null)
                    break;

                if (line.startsWith("#"))
                    continue;

                // 0 qid:1 1:81.0 2:86.0 3:122.0 4:122.0 5:50.0 6:86.0 7:86.0 8:3.0 9:3.0 10:81.0 11:81.0 12:18.444643 13:247.42574 14:3.0 15:827.0 16:3.4250813 17:2.9271529 18:7.4784217 19:-2.817856 20:17.85046 # clueweb09-en0010-79-02218
                int i = line.indexOf("#");

                if (i == -1) {
                    throw new RuntimeException("cannot find # in " + line);
                }

                String docId = line.substring(i + 1).trim();

                int qId = Integer.parseInt(whiteSpaceSplitter.split(line)[1].substring(4));

                ArrayList<String> list = resultList.getOrDefault(qId, new ArrayList<>(numHits));
                list.add(docId);
                resultList.put(qId, list);
            }
        }

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve(Evaluator.prettyModel(similarity.toString()) + "_" + field + ".features"), StandardCharsets.US_ASCII));


        Map<String, TermStatistics> termStatisticsMap = new HashMap<>();


        for (InfoNeed need : track.getTopics()) {

            Analyzer analyzer = "url".equals(field) ? new SimpleAnalyzer() : Analyzers.analyzer(analyzerTag);

            List<String> subParts = Analyzers.getAnalyzedTokens(need.query(), analyzer);

            for (String word : subParts) {
                if (termStatisticsMap.containsKey(word)) continue;
                Term term = new Term(field, word);
                TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
                termStatisticsMap.put(word, termStatistics);
            }

            List<String> docList = resultList.get(need.id());

            if (docList == null || docList.isEmpty()) {
                //out.print(need.id()); out.print(dataSet.getNoDocumentsID());
                continue;
            }

            LinkedHashMap<String, List<DocTermStat>> map = new LinkedHashMap<>(numHits);


            for (String doc : docList) {
                map.put(doc, new ArrayList<>(subParts.size()));
            }

            for (String word : subParts)
                findDoc(map, word, field, searcher);

            for (Map.Entry<String, List<DocTermStat>> entry : map.entrySet()) {

                final int judge = need.getJudgeMap().getOrDefault(entry.getKey(), 0);

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

                }

                // TODO consider dumping field length as we did for the whole document

                out.print("# ");
                out.print(entry.getKey());
                out.println();
                out.flush();
            }

            subParts.clear();
            map.clear();
        }

        termStatisticsMap.clear();
        resultList.clear();
        out.flush();
        out.close();
    }
}
