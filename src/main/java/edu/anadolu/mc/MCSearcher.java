package edu.anadolu.mc;

import edu.anadolu.Searcher;
import edu.anadolu.cmdline.SearcherTool;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DLH13;
import edu.anadolu.similarities.DPH;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.Similarity;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.anadolu.Indexer.FIELD_CONTENTS;
import static edu.anadolu.Indexer.FIELD_ID;
import static edu.anadolu.cmdline.ParamTool.train;

/**
 * Searcher for Milliyet Collection
 */
public class MCSearcher extends Searcher {

    public MCSearcher(Path indexPath, DataSet dataSet) throws Exception {
        super(indexPath, dataSet, 1000);
    }

    public void search(Track track, Similarity similarity, QueryParser.Operator operator, String runsPath) throws Exception {

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);

        final Path path = Paths.get(dataSet.collectionPath().toString(), runsPath, indexTag, track.toString());
        createDirectories(path);


        // for (int part = 0; part <= topics.getMaxParts(); part++)
        int part = 0;

        final String runTag = toString(similarity, operator, FIELD_CONTENTS, part);

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(
                path.resolve(runTag + ".txt"),
                StandardCharsets.US_ASCII));


        QueryParser queryParser = new QueryParser(FIELD_CONTENTS, MCIndexer.analyzer());
        queryParser.setDefaultOperator(operator);


        for (InfoNeed need : track.getTopics()) {
            Query query = queryParser.parse(need.query());


            ScoreDoc[] hits = searcher.search(query, 1000).scoreDocs;

            if (hits.length == 0) {

                out.print(need.id());
                out.print("\tQ0\tMilliyet_0105_v00_00000\t0\t0\t");
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
                Document doc = searcher.doc(docId);
                out.print(need.id());
                out.print("\tQ0\tMilliyet_0105_v00_");
                out.print(doc.get(FIELD_ID));
                out.print("\t");
                out.print(i);
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

    public static void parameterize() throws Exception {


        List<ModelBase> models = SearcherTool.parametricModelList();


        String tfd_home = "/Users/iorixxx/TFD_HOME";

        String tag = "NS";


        MCSet mcSet = new MCSet(tfd_home);


        Path indexPath = Paths.get(mcSet.collectionPath().toString(), "indexes", tag);

        try (MCSearcher searcher = new MCSearcher(indexPath, mcSet)) {
            for (final ModelBase model : models) {
                for (Track track : mcSet.tracks()) {
                    searcher.search(track, model, QueryParser.Operator.AND, "parameter_runs");
                    searcher.search(track, model, QueryParser.Operator.OR, "parameter_runs");
                }
            }
        }
    }

    public final static String[] parametricModels = {"BM25", "LGD", "PL2", "DirichletLM"};
    protected final static String[] operators = {"AND", "OR"};

    public static void main(String[] args) throws Exception {
        search();
        //parameterize();
    }

    public static void search() throws Exception {


        final Set<ModelBase> modelBaseList = new HashSet<>();

        String tfd_home = "/Users/iorixxx/TFD_HOME";

        String tag = "NS";


        MCSet mcSet = new MCSet(tfd_home);

        for (final String op : operators)

            for (Measure measure : Measure.values()) {
                for (String parametricModel : parametricModels) {
                    ModelBase modelBase = train(parametricModel, mcSet, tag, measure, op);
                    modelBaseList.add(modelBase);
                    System.out.println(op + " " + measure.toString() + " " + modelBase);
                }

            }

        modelBaseList.add(new DFIC());
        modelBaseList.add(new DPH());
        modelBaseList.add(new DLH13());
        modelBaseList.add(new DFRee());

        Path indexPath = Paths.get(mcSet.collectionPath().toString(), "indexes", tag);

        try (MCSearcher searcher = new MCSearcher(indexPath, mcSet)) {

            for (final ModelBase model : modelBaseList) {
                for (Track track : mcSet.tracks()) {
                    searcher.search(track, model, QueryParser.Operator.AND, "runs");
                    searcher.search(track, model, QueryParser.Operator.OR, "runs");
                }
            }
        }
    }
}
