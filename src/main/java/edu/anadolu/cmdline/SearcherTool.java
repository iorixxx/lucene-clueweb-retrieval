package edu.anadolu.cmdline;

import edu.anadolu.FeatureSearcher;
import edu.anadolu.Searcher;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.exp.Prob;
import edu.anadolu.exp.Prob1;
import edu.anadolu.exp.Prob2;
import edu.anadolu.exp.Prob3;
import edu.anadolu.freq.L1;
import edu.anadolu.freq.L2;
import edu.anadolu.freq.TFNormalization;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.similarities.ModelBase;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.util.*;

import static edu.anadolu.Indexer.FIELD_CONTENTS;
import static edu.anadolu.cmdline.ParamTool.train;

/**
 * Searcher Tool for Gov2 ClueWeb09 ClueWeb12
 * 300 Topics from TREC 2009-2014 Web Track
 * 150 Topics from TREC 2004-2006 Terabyte Track
 */
public final class SearcherTool extends CmdLineTool {

    static final double[] cValues = new double[]{0.25, 0.5, 0.8, 1, 2, 3, 5, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
    static final double[] kValues = new double[]{0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0};
    static final double[] bValues = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
    static final double[] muValues = new double[]{10, 50, 100, 200, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000};

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Option(name = "-field", usage = "Boolean switch to search different document representations")
    private boolean field = false;

    @Option(name = "-tag", usage = "If you want to search specific tag, e.g. KStemField")
    private String tag = null;

    @Override
    public String getShortDescription() {
        return "Searcher Tool for Gov2 ClueWeb09 ClueWeb12";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    public static List<ModelBase> parametricModelList() {

        List<ModelBase> models = new ArrayList<>();

        for (double c : cValues) {
            models.add(new PL2c(c));
            models.add(new LGDc(c));
        }

        for (double k : kValues)
            for (double b : bValues)
                models.add(new BM25c(k, b));

        for (double mu : muValues)
            models.add(new DirichletLM(mu));

        return models;
    }

    private final Comparator<ModelBase> comparator = Comparator.comparing(ModelBase::toString);

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        final List<String> fields;
        if (field) {
            final String[] arr = props.getProperty(collection.toString() + ".fields", "description,keywords,title,body,anchor,url").split(",");
            fields = Arrays.asList(arr);
        } else {
            fields = new ArrayList<>();
            fields.add(FIELD_CONTENTS);
        }


        QueryParser.Operator[] operators = new QueryParser.Operator[]{QueryParser.Operator.AND, QueryParser.Operator.OR};

        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));


        if ("x".equals(task)) {


            final long start = System.nanoTime();
            Prob[] probs = {new Prob1(), new Prob2(), new Prob3()};
            TFNormalization[] normalizations = {new L1(), new L2()};

            for (final Path path : discoverIndexes(dataset)) {

                Searcher searcher = new Searcher(path, dataset, 1000);

                for (final Track track : dataset.tracks())
                    for (TFNormalization normalization : normalizations)
                        for (Prob prob : probs) {
                            LGDX model = new LGDX(normalization, "KStemAnalyzer", tfd_home, prob);
                            for (final QueryParser.Operator operator : operators)
                                //searcher.search(track, model, operator, false);
                                model.close();
                        }

                searcher.close();

            }
            System.out.println("Search is completed in " + execution(start));
            return;
        } else if ("sota".equals(task)) {

            final long start = System.nanoTime();

            for (final Path path : discoverIndexes(dataset)) {

                Searcher searcher = new Searcher(path, dataset, 1000);

                for (Track track : dataset.tracks())
                    for (ModelBase model : Models.values())
                        searcher.sota(track, model);

                searcher.close();
            }
            System.out.println("SOTA Search is completed in " + execution(start));
            return;
        } else if ("param".equals(task)) {

            final long start = System.nanoTime();

            List<ModelBase> models = parametricModelList();

            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;

                try (Searcher searcher = new Searcher(path, dataset, 1000)) {
                    searcher.searchWithThreads(numThreads, models, fields, "parameter_runs");
                }
            }
            System.out.println("Parameterized Search completed in " + execution(start));
            return;
        } else if ("matf".equals(task)) {

            final long start = System.nanoTime();
            System.out.println("Multi-Aspect TF Search started...");

            for (final Path path : discoverIndexes(dataset)) {
                try (final Searcher searcher = new Searcher(path, dataset, 1000)) {
                    final MATF matf = new MATF();
                    for (final Track track : dataset.tracks()) {
                        searcher.search(track, matf, QueryParser.Operator.OR);
                        searcher.search(track, matf, QueryParser.Operator.AND);
                    }
                }
            }

            System.out.println("Multi-Aspect TF Search completed in " + execution(start));
            return;
        } else if ("spam".equals(task)) {

            final long start = System.nanoTime();

            if (dataset.spamAvailable()) {
                for (final Path path : discoverIndexes(dataset)) {

                    final String tag = path.getFileName().toString();

                    // search for a specific tag, skip the rest
                    if (this.tag != null && !tag.equals(this.tag)) continue;

                    final Set<ModelBase> modelBaseList = new HashSet<>();

                    if (!field)
                        for (String parametricModel : parametricModels)
                            for (Measure measure : Measure.values())
                                modelBaseList.add(train(parametricModel, dataset, tag, measure, "OR"));

                    modelBaseList.add(new DFIC());
                    modelBaseList.add(new DPH());
                    modelBaseList.add(new DLH13());
                    modelBaseList.add(new DFRee());

                    try (Searcher searcher = new Searcher(path, dataset, 10000)) {
                        searcher.searchWithThreads(numThreads, modelBaseList, fields, "base_spam_runs");
                    }
                    modelBaseList.clear();
                }

                System.out.println("Base search for spam filtering 10,000 documents per query completed in " + execution(start));
                return;
            }
        } else if ("feature".equals(task)) {

            final long start = System.nanoTime();

            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;

                final TreeSet<ModelBase> modelBaseList = new TreeSet<>(Comparator.comparing(ModelBase::toString));

                for (String parametricModel : parametricModels)
                    modelBaseList.add(train(parametricModel, dataset, tag, Measure.NDCG1000, "OR"));

                modelBaseList.add(new DFIC());
                modelBaseList.add(new DPH());
                modelBaseList.add(new DLH13());
                modelBaseList.add(new DFRee());

                try (FeatureSearcher searcher = new FeatureSearcher(path, dataset, 1000)) {
                    searcher.searchF(modelBaseList, "features");
                }
                modelBaseList.clear();
            }

            System.out.println("Feature extraction completed in " + execution(start));
            return;
        } else if ("field".equals(task)) {

            final long start = System.nanoTime();

            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                // Field based feature extraction is available only KStemField
                if (!tag.equals("KStemField")) continue;

                final TreeSet<ModelBase> modelBaseList = new TreeSet<>(Comparator.comparing(ModelBase::toString));

                for (String parametricModel : parametricModels)
                    modelBaseList.add(train(parametricModel, dataset, "KStem", Measure.NDCG1000, "OR"));

                modelBaseList.add(new DFIC());
                modelBaseList.add(new DPH());
                modelBaseList.add(new DLH13());
                modelBaseList.add(new DFRee());

                try (FeatureSearcher searcher = new FeatureSearcher(path, dataset, 1000)) {
                    searcher.searchF(modelBaseList, Arrays.asList("anchor", "body", "title", "url"));
                }
                modelBaseList.clear();
            }

            System.out.println("Field based feature extraction completed in " + execution(start));
            return;
        }

        final long start = System.nanoTime();

        for (final Path path : discoverIndexes(dataset)) {

            final String tag = path.getFileName().toString();

            // search for a specific tag, skip the rest
            if (this.tag != null && !tag.equals(this.tag)) continue;

            final Set<ModelBase> modelBaseList = new HashSet<>();
            if (!field)
                for (String parametricModel : parametricModels)
                    for (Measure measure : Measure.values())
                        modelBaseList.add(train(parametricModel, dataset, tag, measure, "OR"));

            modelBaseList.add(new DFIC());
            modelBaseList.add(new DPH());
            modelBaseList.add(new DLH13());
            modelBaseList.add(new DFRee());

            try (Searcher searcher = new Searcher(path, dataset, 1000)) {
                searcher.searchWithThreads(numThreads, modelBaseList, fields, "runs");
            }
            modelBaseList.clear();
        }

        System.out.println("Search completed in " + execution(start));

    }
}