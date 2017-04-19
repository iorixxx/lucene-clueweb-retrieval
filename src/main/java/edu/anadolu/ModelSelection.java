package edu.anadolu;

import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import edu.anadolu.qpp.Aggregate;
import edu.anadolu.qpp.IDF;
import edu.anadolu.qpp.Scope;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.lucene.queryparser.classic.ParseException;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Model selection proposed by He and Ounis (2003, 2004)
 */
public class ModelSelection {


    public double[] getFeatureVector(InfoNeed need) {
        return queryVectors.get(need);
    }

    private final Map<InfoNeed, double[]> queryVectors = new HashMap<>();

    public ModelSelection(DataSet dataSet, String tag) throws IOException, ParseException {


        final List<InfoNeed> allQueries = Collections.unmodifiableList(new QueryBank(dataSet).getAllQueries(0));

        Path cacheFile = dataSet.collectionPath().resolve(tag + ".features");

        if (Files.exists(cacheFile)) {

            List<String> lines = Files.readAllLines(cacheFile, StandardCharsets.US_ASCII);

            if (allQueries.size() != lines.size()) {
                throw new RuntimeException("all queries size does not equal to lines");
            }

            for (int i = 0; i < allQueries.size(); i++) {

                InfoNeed need = allQueries.get(i);
                double[] vector = new double[3];
                String[] parts = Track.whiteSpaceSplitter.split(lines.get(i));

                if (!("qid:" + need.id()).equals(parts[0]))
                    throw new RuntimeException(parts[0] + " does not equal qid:" + need.id());

                if (parts.length != 4) throw new RuntimeException("line parts is not 4!");

                vector[0] = Double.parseDouble(parts[1]);
                vector[1] = Double.parseDouble(parts[2]);
                vector[2] = Double.parseDouble(parts[3]);
                queryVectors.put(need, vector);
            }

            lines.clear();
            return;
        }

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(cacheFile, StandardCharsets.US_ASCII));

        Path indexPath = dataSet.indexesPath().resolve(tag);
        BigDecimal p = new BigDecimal("0.2");
        try (Scope scope = new Scope(indexPath); IDF idf = new IDF(indexPath)) {

            for (InfoNeed need : allQueries) {
                double[] vector = new double[3];
                out.print("qid:");
                out.print(need.id());
                out.print("\t");
                vector[0] = new BigDecimal(need.wordCount()).multiply(p).doubleValue();
                out.print(vector[0]);
                out.print("\t");
                vector[1] = idf.aggregated(need, new Aggregate.Gamma1());
                out.print(vector[1]);
                out.print("\t");
                vector[2] = scope.value(need);
                out.print(vector[2]);
                out.println();
                out.flush();
                queryVectors.put(need, vector);
            }
        }

        out.close();
    }

    public void printSolutionList() {

        Collections.sort(solutionList, (o1, o2) -> Double.compare(o2.sigma1, o1.sigma1));

        for (Solution solution : solutionList)
            System.out.println(solution.k + "\t" + String.format("%.2f", solution.sigma1) + "\t" + String.format("%.5f", solution.getMean()));

        System.out.println();
    }

    private final List<Solution> solutionList = new ArrayList<>();

    public List<Solution> solutionList() {
        return solutionList;
    }

    public Solution evaluate(Evaluator evaluator) {

        List<InfoNeed> residualNeeds = evaluator.residualNeeds();


        for (int k = 2; k <= 10; k++) {
            List<Prediction> predictionList = new ArrayList<>();

            for (InfoNeed testQuery : residualNeeds) {

                List<InfoNeed> trainingQueries = jackKnife(residualNeeds, testQuery);

                if (trainingQueries.size() != residualNeeds.size() - 1)
                    throw new RuntimeException("train test size mismatch!");

                Prediction prediction = process(trainingQueries, testQuery, k, evaluator);
                predictionList.add(prediction);

                trainingQueries.clear();

            }

            Solution solution = new Solution(predictionList, k);
            evaluator.calculateAccuracy(solution);
            solution.model = "MS" + k;
            solution.key = "MS" + k;
            solutionList.add(solution);
        }


        return Collections.max(solutionList, (o1, o2) -> Double.compare(o1.sigma1, o2.sigma1));


    }

    static List<InfoNeed> trainingQueries(List<InfoNeed> residualNeeds, Track track) {
        return Collections.unmodifiableList(
                residualNeeds.stream()
                        .filter(need -> need.getWT() != track)
                        .collect(Collectors.toList()
                        )
        );
    }

    static List<InfoNeed> testQueries(List<InfoNeed> residualNeeds, Track track) {
        return Collections.unmodifiableList(
                residualNeeds.stream()
                        .filter(need -> need.getWT() == track)
                        .collect(Collectors.toList()
                        )
        );
    }

    private static List<InfoNeed> jackKnife(List<InfoNeed> residualNeeds, InfoNeed testQuery) {
        return residualNeeds.stream()
                .filter(need -> need.id() != testQuery.id())
                .collect(Collectors.toList()
                );
    }

    private List<Set<InfoNeed>> splitTrainingSetIntoKClusters(List<InfoNeed> trainingQueries, int k) {

        List<Set<InfoNeed>> tmp = initialClusters(trainingQueries);

        for (int i = 0; i < trainingQueries.size() - k; i++) {
            mergeClosestPairClusters(tmp);
            // displayClusters(clusters);
        }

        if (tmp.size() != k)
            throw new RuntimeException("tmp.size !=  k " + k);

        return tmp;

    }


    private Solution process(List<InfoNeed> trainingQueries, List<InfoNeed> testQueries, int k, Evaluator evaluator) {

        List<Set<InfoNeed>> clusters = splitTrainingSetIntoKClusters(trainingQueries, k);

        if (clusters.size() != k) throw new RuntimeException("expect the unexpected");

        /**
         * Model Selection Mechanism
         */
        List<String> bestModelPerCluster = new ArrayList<>(clusters.size());

        for (int i = 0; i < clusters.size(); i++) {
            Set<InfoNeed> cluster = clusters.get(i);
            List<ModelScore> list = evaluator.averageForAllModels(cluster);
            bestModelPerCluster.add(i, Collections.max(list, (o1, o2) -> Double.compare(o1.score, o2.score)).model);
        }

        if (bestModelPerCluster.size() != clusters.size()) throw new RuntimeException("expect the unexpected");


        List<Prediction> predictionList = new ArrayList<>();
        for (InfoNeed need : testQueries) {

            final int clusterID = clusterBelongsTo(clusters, need);

            final String predictedModel = bestModelPerCluster.get(clusterID);


            // System.out.println(need + " " + predictedModel);

            double predictedScore = evaluator.score(need, predictedModel);

            Prediction prediction = new Prediction(need, predictedModel, predictedScore);
            predictionList.add(prediction);

        }

        Solution solution = new Solution(predictionList, k);
        evaluator.calculateAccuracy(solution);
        return solution;

    }

    private Prediction process(List<InfoNeed> trainingQueries, InfoNeed testQuery, int k, Evaluator evaluator) {

        List<Set<InfoNeed>> clusters = splitTrainingSetIntoKClusters(trainingQueries, k);

        if (clusters.size() != k) throw new RuntimeException("expect the unexpected");

        /**
         * Model Selection Mechanism
         */
        List<String> bestModelPerCluster = new ArrayList<>(clusters.size());

        for (int i = 0; i < clusters.size(); i++) {
            Set<InfoNeed> cluster = clusters.get(i);
            List<ModelScore> list = evaluator.averageForAllModels(cluster);
            bestModelPerCluster.add(i, Collections.max(list, (o1, o2) -> Double.compare(o1.score, o2.score)).model);
        }

        if (bestModelPerCluster.size() != clusters.size()) throw new RuntimeException("expect the unexpected");


        final int clusterID = clusterBelongsTo(clusters, testQuery);

        final String predictedModel = bestModelPerCluster.get(clusterID);


        // System.out.println(need + " " + predictedModel);

        double predictedScore = evaluator.score(testQuery, predictedModel);

        bestModelPerCluster.clear();
        clusters.clear();
        return new Prediction(testQuery, predictedModel, predictedScore);

    }

    /**
     * Assign unseen query to a cluster
     *
     * @param clusters    k clusters obtained in training phase
     * @param unseenQuery unseen test query
     * @return cluster id
     */
    private int clusterBelongsTo(List<Set<InfoNeed>> clusters, InfoNeed unseenQuery) {

        double closest = Double.NEGATIVE_INFINITY;

        int clusterID = -1;

        final double[] testQueryFeatures = queryVectors.get(unseenQuery);

        for (int i = 0; i < clusters.size(); i++)
            for (InfoNeed need : clusters.get(i)) {

                if (need.id() == unseenQuery.id())
                    throw new RuntimeException("unseen test query shouldn't be in the clusters!");

                final double sim = cosineSimilarity(queryVectors.get(need), testQueryFeatures);

                if (sim > closest) {
                    closest = sim;
                    clusterID = i;
                }
            }

        if (clusterID == -1) throw new RuntimeException("expect the unexpected!");

        return clusterID;
    }

    /**
     * initially, each vector is an independent cluster.
     *
     * @param needs training queries
     * @return clusters
     */
    private static List<Set<InfoNeed>> initialClusters(List<InfoNeed> needs) {
        List<Set<InfoNeed>> clusters = new ArrayList<>(needs.size());

        for (InfoNeed need : needs) {
            Set<InfoNeed> set = new HashSet<>();
            set.add(need);
            clusters.add(set);
        }

        if (clusters.size() != needs.size())
            throw new RuntimeException("initial clusters size is different from needs size!");

        return clusters;
    }

    private void mergeClosestPairClusters(List<Set<InfoNeed>> clusters) {

        double closest = Double.NEGATIVE_INFINITY;

        int p1 = -1, p2 = -1;

        for (int i = 0; i < clusters.size(); i++) {
            Set<InfoNeed> cluster1 = clusters.get(i);

            for (int j = 0; j < clusters.size(); j++) {

                if (i == j) continue;

                Set<InfoNeed> cluster2 = clusters.get(j);
                double sim = compare(cluster1, cluster2);

                if (sim > closest) {
                    closest = sim;
                    p1 = i;
                    p2 = j;
                }
            }
        }

        if (p1 == -1 || p2 == -1) throw new RuntimeException("p1 = " + p1 + " p2 = " + p2);

        // System.out.println("p1 = " + p1 + " p2 = " + p2 + " closest = " + closest);

        clusters.get(p1).addAll(clusters.get(p2));

        clusters.remove(p2);
    }

    private static double cosineSimilarity(double[] vectorA, double[] vectorB) {

        if (vectorA.length != vectorB.length) throw new RuntimeException("two vectors must have the same length!");

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * The similarity between two clusters is measured by
     * the cosine similarity of the two closest vectors (having the highest cosine similarity), where
     * the two vectors come from each cluster respectively. If
     *
     * @param o1 cluster1
     * @param o2 cluster2
     * @return cluster similarity
     */
    private double compare(Set<InfoNeed> o1, Set<InfoNeed> o2) {

        SummaryStatistics summaryStatistics = new SummaryStatistics();

        for (InfoNeed need1 : o1)
            for (InfoNeed need2 : o2) {
                if (need1.id() == need2.id())
                    throw new RuntimeException("info need ids are equal!");
                summaryStatistics.addValue(cosineSimilarity(queryVectors.get(need1), queryVectors.get(need2)));
            }

        return summaryStatistics.getMax();

    }

    private static void displayClusters(List<Set<InfoNeed>> clusters) {
        int i = 0;
        for (Set<InfoNeed> cluster : clusters) {
            System.out.println((i++) + "\t" + cluster.toString());
        }
    }

}
