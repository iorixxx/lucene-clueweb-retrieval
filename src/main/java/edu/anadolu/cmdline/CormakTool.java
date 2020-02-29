package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.CormakEvaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.spam.SubmissionFile;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.anadolu.cmdline.RocTool.Ranking.fusion;
import static edu.anadolu.cmdline.SpamTool.getSpamSolr;
import static edu.anadolu.cmdline.SpamTool.percentile;

/**
 * Tool for integration Waterloo Spam Rankings
 */
public final class CormakTool extends CmdLineTool {


    @Option(name = "-rank", usage = "Spam Ranking")
    private RocTool.Ranking ranking = RocTool.Ranking.fusion;

    @Option(name = "-random", usage = "Random Filtering")
    private boolean random = false;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-catA", usage = "Use Category A submissions")
    private boolean catA = false;

    @Override
    public String getShortDescription() {
        return "Tool for integration Waterloo Spam Rankings";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.spam paths.docs files.ids files.spam";
    }

    private DataSet dataset;

    static private class WT09 extends DataSet {

        WT09(String tfd_home) {
            super(Collection.MQ09, new Track[]{
                    new org.clueweb09.tracks.WT09(tfd_home)
            }, tfd_home);
        }

        @Override
        public Path indexesPath() {
            return Paths.get(tfd_home, Collection.CW09A.toString(), "indexes");
        }

        @Override
        public String getNoDocumentsID() {
            return "clueweb09-en0000-00-00000";
        }

        @Override
        public boolean spamAvailable() {
            return true;
        }
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        dataset = new WT09(tfd_home);

        if ("spam".equals(task)) {
            spam();
            return;
        }

        final String solrBaseURL = props.getProperty("SOLR.URL");

        if (solrBaseURL == null) {
            System.out.println(getHelp());
            return;
        }

        final HttpSolrClient solr;
        if (fusion.equals(ranking))
            solr = getSpamSolr(Collection.CW09A, solrBaseURL);
        else
            solr = new HttpSolrClient.Builder().withBaseSolrUrl(solrBaseURL + ranking.toString()).build();

        if (solr == null) return;

        List<Path> pathList = discoverSubmissions(Paths.get(tfd_home, Collection.MQ09.toString()).resolve("base_spam_runs"));

        System.out.println("there are " + pathList.size() + " many TREC submission files found to be processed...");

        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "4"));
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);

        long start = System.nanoTime();

        for (Path submission : pathList) {
            executor.execute(() -> {
                try {
                    if (this.random) {
                        for (int threshold = 10; threshold <= 90; threshold += 10)
                            random(dataset, submission, threshold);
                    } else
                        filterTRECSubmissionFile(dataset, submission, solr, ranking);
                } catch (SolrServerException | IOException ioe) {
                    System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                    ioe.printStackTrace();
                }
            });

        }

        //add some delay to let some threads spawn by scheduler
        Thread.sleep(30000);
        executor.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait for existing tasks to terminate
            while (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                Thread.sleep(1000);
                System.out.println(String.format("%.2f percentage completed in ", (double) executor.getCompletedTaskCount() / executor.getTaskCount() * 100.0d) + execution(start));
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        System.out.println("Percolator completed in " + execution(start));
        solr.close();
    }

    public static List<Path> discoverSubmissions(Path path) throws IOException {
        return Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("input"))
                .collect(Collectors.toList());
    }

    /**
     * Filter documents from a TREC submission file
     */
    private static void filterTRECSubmissionFile(DataSet dataset, Path submission, HttpSolrClient solr, RocTool.Ranking ranking) throws IOException, SolrServerException {

        final SubmissionFile submissionFile = new SubmissionFile(submission);

        Path relPath = dataset.collectionPath().resolve("base_spam_runs").relativize(submission);

        Map<Integer, List<SubmissionFile.Tuple>> submissionFileMap = submissionFile.entryMap();
        String runTag = submissionFile.runTag();

        Map<Integer, PrintWriter> writerMap = new HashMap<>(19);

        for (int threshold = 10; threshold <= 90; threshold += 10) {

            Path parallel = dataset.collectionPath().resolve("spam_" + ranking + "_" + threshold + "_runs").resolve(relPath);

            if (!Files.exists(parallel.getParent()))
                Files.createDirectories(parallel.getParent());

            PrintWriter out = new PrintWriter(Files.newBufferedWriter(parallel, StandardCharsets.US_ASCII));
            writerMap.put(threshold, out);

        }

        for (Map.Entry<Integer, List<SubmissionFile.Tuple>> entry : submissionFileMap.entrySet()) {

            Integer qID = entry.getKey();

            Map<Integer, Integer> countMap = new HashMap<>(19);

            for (int threshold = 10; threshold <= 90; threshold += 10) {
                countMap.put(threshold, 0);
            }

            List<SubmissionFile.Tuple> list = entry.getValue();

            for (SubmissionFile.Tuple tuple : list) {

                int percentile = percentile(solr, tuple.docID);

                for (int threshold = 10; threshold <= 90; threshold += 10) {

                    if (percentile < threshold || countMap.get(threshold) == 1000) {
                        continue;
                    }

                    int counter = countMap.get(threshold) + 1;
                    countMap.put(threshold, counter);

                    writerMap.get(threshold).print(qID);
                    writerMap.get(threshold).print("\tQ0\t");
                    writerMap.get(threshold).print(tuple.docID);
                    writerMap.get(threshold).print("\t");
                    writerMap.get(threshold).print(counter);
                    writerMap.get(threshold).print("\t");
                    writerMap.get(threshold).print(tuple.score);
                    writerMap.get(threshold).print("\t");
                    writerMap.get(threshold).print(runTag);
                    writerMap.get(threshold).println();


                }
            }

            /*
             * TREC submission system requires you to submit documents for every topic.
             * If there are no documents for a certain topic, please insert 'clueweb12-0000wb-00-00000' as DOC-ID with a dummy score.
             * If you are returning zero documents for a query, instead return the single document "clueweb09-en0000-00-00000".
             */
            for (int threshold = 10; threshold <= 90; threshold += 10) {
                if (countMap.get(threshold) == 0) {
                    writerMap.get(threshold).println(qID + "\tQ0\t" + dataset.getNoDocumentsID() + "\t1\t0\t" + runTag);
                }
            }
        }

        for (int threshold = 10; threshold <= 90; threshold += 10) {
            writerMap.get(threshold).flush();
            writerMap.get(threshold).close();
        }

        writerMap.clear();
        submissionFile.clear();
    }

    /**
     * Randomly Filter documents from a TREC submission file
     */
    private static void random(DataSet dataset, Path submission, int i) throws IOException {

        final SubmissionFile submissionFile = new SubmissionFile(submission);

        Path relPath = dataset.collectionPath().resolve("base_spam_runs").relativize(submission);

        Map<Integer, List<SubmissionFile.Tuple>> submissionFileMap = submissionFile.entryMap();
        String runTag = submissionFile.runTag();


        Path parallel = dataset.collectionPath().resolve("spam_random_" + i + "_runs").resolve(relPath);

        if (!Files.exists(parallel.getParent()))
            Files.createDirectories(parallel.getParent());

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(parallel, StandardCharsets.US_ASCII));


        for (Map.Entry<Integer, List<SubmissionFile.Tuple>> entry : submissionFileMap.entrySet()) {

            Integer qID = entry.getKey();

            int countMap = 0;


            List<SubmissionFile.Tuple> list = entry.getValue();

            for (SubmissionFile.Tuple tuple : list) {


                boolean filter = ThreadLocalRandom.current().nextBoolean();

                if (filter || countMap == 1000) {
                    continue;
                }

                countMap++;

                out.print(qID);
                out.print("\tQ0\t");
                out.print(tuple.docID);
                out.print("\t");
                out.print(countMap);
                out.print("\t");
                out.print(tuple.score);
                out.print("\t");
                out.print(runTag);
                out.println();
            }

            /*
             * TREC submission system requires you to submit documents for every topic.
             * If there are no documents for a certain topic, please insert 'clueweb12-0000wb-00-00000' as DOC-ID with a dummy score.
             * If you are returning zero documents for a query, instead return the single document "clueweb09-en0000-00-00000".
             */

            if (countMap == 0) {
                out.println(qID + "\tQ0\t" + dataset.getNoDocumentsID() + "\t1\t0\t" + runTag);
            }
        }

        out.flush();
        out.close();
    }

    public void spam() {
        SortedMap<Integer, List<ModelScore>> map = new TreeMap<>();

        CormakEvaluator evaluator = new CormakEvaluator(this.catA, dataset, "KStem", Measure.P10, "input*", "evals", "OR");


        double[] scores = new double[10];
        int maxSpam = 0;
        double max = evaluator.averageOfAllModels(SpamEvalTool.AGG.M);

        scores[0] = max;

        System.out.print(String.format("%.5f", max) + "\tspamThreshold = 0\t");
        evaluator.printMean();
        map.put(0, evaluator.averageForAllModels());
        System.out.println("=======================");

        int c = 1;
        for (int threshold = 10; threshold <= 90; threshold += 10) {


            String evalDir = random ? "spam_random_" + threshold + "_evals" : "spam_" + ranking + "_" + threshold + "_evals";
            evaluator = new CormakEvaluator(this.catA, dataset, "KStem", Measure.P10, "input*", evalDir, "OR");

            double mean = evaluator.averageOfAllModels(SpamEvalTool.AGG.M);

            scores[c++] = mean;

            System.out.print(String.format("%.5f", mean) + "\tspamThreshold = " + threshold + "\t");
            evaluator.printMean();
            map.put(threshold, evaluator.averageForAllModels());
            System.out.println("=======================");

            if (mean > max) {
                max = mean;
                maxSpam = threshold;
            }
        }

        System.out.println("================= Best threshold is " + maxSpam + " =======================" + "Aggregated with " + SpamEvalTool.AGG.M);

        for (double d : scores)
            System.out.print(d + "\t");

    }

}