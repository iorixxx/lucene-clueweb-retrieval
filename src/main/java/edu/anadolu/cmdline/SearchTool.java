package edu.anadolu.cmdline;

import edu.anadolu.Searcher;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.*;
import org.apache.lucene.search.similarities.ModelBase;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static edu.anadolu.cmdline.SpamEvalTool.display;

/**
 * ./run.sh Evaluator -collection CW09A -models BM25k1.0b0.4_DirichletLMc500.0_LGDc2.0_DFIC_DLH13_PL2c3.0_DFRee_DPH -tag 80_KStem
 */
public final class SearchTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    private int spam = 0;

    @Option(name = "-i", required = false, usage = "increments of spam threshold", metaVar = "5 10 20")
    private int i = 20;

    @Option(name = "-task", required = false, usage = "task to be executed: search or eval")
    private String task;

    @Override
    public String getShortDescription() {
        return "Searcher Tool for ClueWeb09 indices indexed without spam";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    private String tag = null;

    private final Set<ModelBase> modelBaseList = new HashSet<>();

    SearchTool() {
        modelBaseList.add(new DFIC());
        modelBaseList.add(new DPH());
        modelBaseList.add(new DLH13());
        modelBaseList.add(new DFRee());
        modelBaseList.add(new BM25c(1.0, 0.4));
        modelBaseList.add(new LGDc(2.0));
        modelBaseList.add(new PL2c(3.0));
        modelBaseList.add(new DirichletLM(500.0));
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);


        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));

        if ("search".equals(task)) {
            if (spam > 0)
                this.tag = spam + "_KStem";

            final long start = System.nanoTime();


            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;


                try (Searcher searcher = new Searcher(path, dataset, 1000)) {
                    searcher.searchWithThreads(numThreads, modelBaseList, Collections.singletonList("contents"), "runs");
                }


            }
            System.out.println("Search completed in " + execution(start));
            return;
        }

        if ("eval".equals(task)) {

            SortedMap<Integer, List<ModelScore>> map = new TreeMap<>();

            final String models = Evaluator.models(modelBaseList.stream().map(ModelBase::toString).collect(Collectors.toList()));
            System.out.println(models);

            Evaluator evaluator = new Evaluator(dataset, "KStem", Measure.NDCG100, models, "evals", "OR");
            evaluator.models();

            int maxSpam = 0;
            double max = evaluator.averageOfAllModels(SpamEvalTool.AGG.M);

            System.out.print(String.format("%.5f", max) + "\tspamThreshold = 0\t");
            evaluator.printMean();
            map.put(0, evaluator.averageForAllModels());
            System.out.println("=======================");

            for (int spamThreshold = i; spamThreshold < 100; spamThreshold += i) {

                evaluator = new Evaluator(dataset, spamThreshold + "_KStem", Measure.NDCG100, models, "evals", "OR");

                double mean = evaluator.averageOfAllModels(SpamEvalTool.AGG.M);

                System.out.print(String.format("%.5f", mean) + "\tspamThreshold = " + spamThreshold + "\t");
                evaluator.printMean();
                map.put(spamThreshold, evaluator.averageForAllModels());
                System.out.println("=======================");

                if (mean > max) {
                    max = mean;
                    maxSpam = spamThreshold;
                }
            }

            System.out.println("================= Best threshold is " + maxSpam + " =======================" + "Aggregated with " + SpamEvalTool.AGG.M);

            display(map);
        }
    }
}