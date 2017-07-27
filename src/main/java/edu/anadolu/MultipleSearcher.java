package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static edu.anadolu.Indexer.FIELD_CONTENTS;
import static edu.anadolu.Indexer.FIELD_ID;


public class MultipleSearcher {
    protected final DataSet dataSet;
    private Searcher[] searchers;
    private static String indexTag = "SelectiveStemming";
    private int numHits;

    private final class SearcherThread extends Thread {

        private final QueryParser.Operator operator;
        private final String field;
        private final Track track;
        private final Similarity similarity;
        private final Path path;

        SearcherThread(Track track, Similarity similarity, QueryParser.Operator operator, String field, Path path) {
            this.operator = operator;
            this.field = field;
            this.track = track;
            this.similarity = similarity;
            this.path = path;
            setName(indexTag + similarity.toString() + track.toString());
        }

        @Override
        public void run() {
            try {
                search(track, similarity, operator, field, path);
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + ": unexpected exception : " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return indexTag + similarity.toString() + track.toString();
        }

    }

    public MultipleSearcher(Searcher... searchers) {
        this.searchers = searchers;
        this.dataSet=searchers[0].dataSet
        ;this.numHits=searchers[0].numHits;
    }
    public void runSelectiveStemming(){
        Searcher searcher1 = searchers[0];
        Searcher searcher2 = searchers[1];
    }

    public static void createDirectories(Path path) throws IOException {
        if (!Files.exists(path))
            Files.createDirectories(path);
    }

    public static String toString(Similarity similarity, QueryParser.Operator operator, String field, int part) {
        String p = part == 0 ? "all" : Integer.toString(part);
        return similarity.toString().replaceAll(" ", "_") + "_" + field + "_" + indexTag + "_" + operator.toString() + "_" + p;
    }

    public void searchWithThreads(int numThreads, Collection<ModelBase> models, Collection<String> fields, String runsPath) throws InterruptedException, IOException {

        System.out.println("There are " + models.size() * fields.size() * searchers[0].dataSet.tracks().length + " many tasks to process...");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

        for (final Track track : searchers[0].dataSet.tracks()) {

            final Path path = Paths.get(searchers[0].dataSet.collectionPath().toString(), runsPath, indexTag, track.toString());
            createDirectories(path);

            for (String field : fields)
                for (final ModelBase model : models) {
                    //    executor.execute(new SearcherThread(track, model, QueryParser.Operator.AND, path));
                    executor.execute(new SearcherThread(track, model, QueryParser.Operator.OR, field, path));
                }
        }

        //add some delay to let some threads spawn by scheduler
        Thread.sleep(30000);
        executor.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait for existing tasks to terminate
            while (!executor.awaitTermination(3, TimeUnit.MINUTES)) {
                Thread.sleep(1000);
                //System.out.println(String.format("%.2f percentage completed", (double) executor.getCompletedTaskCount() / executor.getTaskCount() * 100.0d));
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        if (executor.getTaskCount() != executor.getCompletedTaskCount())
            throw new RuntimeException("total task count = " + executor.getTaskCount() + " is not equal to completed task count =  " + executor.getCompletedTaskCount());
    }
    public void search(Track track, Similarity similarity, QueryParser.Operator operator, Path path) throws IOException, ParseException {
        search(track, similarity, operator, FIELD_CONTENTS, path);
    }

    public void search(Track track, Similarity similarity, QueryParser.Operator operator, String field, Path path) throws IOException, ParseException {

        //Indexsearcher for each searcher objects and their query parser
        IndexSearcher[] indexSearchers = new IndexSearcher[searchers.length];
        HashMap<IndexSearcher,QueryParser> indexSearcherQueryParser = new HashMap<IndexSearcher,QueryParser>();

        for(Searcher s: searchers){
            IndexSearcher is =  new IndexSearcher(s.reader);
            is.setSimilarity(similarity);

            QueryParser queryParser = new QueryParser(field, Analyzers.analyzer(s.analyzerTag));
            queryParser.setDefaultOperator(operator);

            indexSearcherQueryParser.put(is, queryParser);
        }

        // for (int part = 0; part <= topics.getMaxParts(); part++)
        int part = 0;

        final String runTag = toString(similarity, operator, field, part);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve(runTag + ".txt"), StandardCharsets.US_ASCII));

        for (InfoNeed need : track.getTopics()) {
            IndexSearcher selectedSearcher = SelectionMethods.MST(need,indexSearchers); //Select indexsearcher for each topic
            String queryString = need.getPartOfQuery(part);
            if (queryString == null) continue;
            Query query = indexSearcherQueryParser.get(selectedSearcher).parse(queryString);

            ScoreDoc[] hits = selectedSearcher.search(query, numHits).scoreDocs;

            /**
             * If you are returning zero documents for a query, instead return the single document
             * clueweb09-en0000-00-00000
             * clueweb12-0000wb-00-00000
             * GX000-00-0000000
             * If you are returning zero documents for a query, instead return the single document "clueweb09-en0000-00-00000".
             * If you would normally return no documents for a query, instead return the single document "clueweb09-en0000-00-00000" at rank one.
             * Doing so maintains consistent evaluation results (averages over the same number of queries) and does not break anyone's tools.
             */
            if (hits.length == 0) {

                out.print(need.id());
                out.print("\tQ0\t");
                out.print(dataSet.getNoDocumentsID());
                out.print("\t1\t0\t");
                out.print(runTag);
                out.println();
                continue;
            }

            /**
             * the first column is the topic number.
             * the second column is currently unused and should always be "Q0".
             * the third column is the official document identifier of the retrieved document.
             * the fourth column is the rank the document is retrieved.
             * the fifth column shows the score (integer or floating point) that generated the ranking.
             * the sixth column is called the "run tag" and should be a unique identifier for your
             */
            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                Document doc = selectedSearcher.doc(docId);
                out.print(need.id());
                out.print("\tQ0\t");
                out.print(doc.get(FIELD_ID));
                out.print("\t");
                out.print(i + 1);
                out.print("\t");
                out.print(hits[i].score);
                out.print("\t");
                out.print(runTag);
                out.println();
                out.flush();
            }
        }

        out.close();
    }
}
