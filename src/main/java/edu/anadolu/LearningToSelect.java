package edu.anadolu;

import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import org.apache.commons.math3.stat.StatUtils;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.ModelSelection.testQueries;
import static edu.anadolu.ModelSelection.trainingQueries;
import static org.apache.lucene.search.similarities.ModelBase.log2;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Learning To Select (LTS) framework proposed by Jie Peng (2010)
 */
public class LearningToSelect {

    final static double c = 0.01;

    final String op;

    final String tag;
    private final int rangeK = 96;

    Evaluator evaluator;

    final Path collectionPath;
    final DataSet dataSet;


    public LearningToSelect(DataSet dataSet, String tag, String op) {
        this.collectionPath = dataSet.collectionPath();
        this.tag = tag;
        this.op = op;
        this.dataSet = dataSet;
    }

    Map<String, Double> documentRelevanceScores(String model, InfoNeed need, int n) throws IOException {

        Path thePath = Paths.get(collectionPath.toString(), "runs", tag, need.getWT().toString(), model + "_contents_" + tag + "_" + op + "_all.txt");

        if (!Files.exists(thePath) || !Files.isRegularFile(thePath) || !Files.isReadable(thePath))
            throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

        String queryID = need.id() + "\t";

        Map<String, Double> documentScoreMap = new HashMap<>();

        int counter = 0;
        for (String line : Files.readAllLines(thePath, StandardCharsets.US_ASCII)) {

            if (!line.startsWith(queryID)) continue;

            if (++counter > n) break;

            String[] parts = whiteSpaceSplitter.split(line);


            double score = Double.parseDouble(parts[4]);

            int qID = Integer.parseInt(parts[0]);

            if (qID != need.id())
                throw new RuntimeException("supplied query id " + need.id() + " does not match with the one extracted from the submission file : " + qID);

            String docId = parts[2];

            if (!dataSet.validateDocID(docId))
                throw new RuntimeException("invalid doc id : " + docId);


            documentScoreMap.put(docId, score);

        }

        return Collections.unmodifiableMap(documentScoreMap);
    }

    private class NeedDistance implements Comparable<NeedDistance> {

        final InfoNeed need;
        final double distance;

        public NeedDistance(InfoNeed need, double distance) {
            this.need = need;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return need + " " + distance;
        }

        @Override
        public int compareTo(NeedDistance o) {
            if (o.distance < this.distance) return 1;
            else if (o.distance > this.distance) return -1;
            else
                return 0;
        }
    }

    static Set<String> intersection(Map<String, Double> m1, Map<String, Double> m2) {

        Set<String> s1 = new HashSet<>(m1.keySet());
        Set<String> s2 = new HashSet<>(m2.keySet());
        s1.retainAll(s2);
        return s1;
    }

    /**
     * Score normalization proposed by Lee (1997)
     *
     * @return normalized scores
     */
    static double[] normalizeScores(Set<String> intersection, Map<String, Double> documentRelevanceScoresMap, double c) {

        double[] scores = new double[intersection.size()];

        // TODO how to normalize an array who has only one element? e.g. [-3.501083]
        if (intersection.size() == 1) {
            Arrays.fill(scores, 1.0 + c);
            return scores;
        }

        int counter = 0;

        for (String docId : intersection) {

            final double score = documentRelevanceScoresMap.get(docId);
            scores[counter++] = score;
        }

        double min = StatUtils.min(scores);
        double max = StatUtils.max(scores);

        // TODO how to normalize an array whose elements are the same? e.g. [17.529446, 17.529446]
        if (min == max) {

//            System.out.println(Arrays.toString(scores));
//            System.out.println(documentRelevanceScoresMap);
//            System.out.println(intersection);

            Arrays.fill(scores, c);
            return scores;
        }

        for (int i = 0; i < scores.length; i++) {
            scores[i] = (scores[i] - min) / (max - min) + c;
        }

        return scores;
    }

    static double kullbackLeiber(double[] rb, double[] rc) {

        if (rb.length != rc.length)
            throw new RuntimeException("array sizes are not equal!");

        double kl = 0.0;
        for (int i = 0; i < rb.length; i++) {

            kl += rb[i] * log2(rb[i] / rc[i]);
        }

        return kl / rb.length;
    }

    static double jensenShannon(double[] rb, double[] rc) {

        if (rb.length != rc.length)
            throw new RuntimeException("array sizes are not equal!");

        double js = 0.0;
        for (int i = 0; i < rb.length; i++) {

            js += rb[i] * log2(rb[i] / (0.5 * rb[i] + 0.5 * rc[i]));
        }

        return js / rb.length;
    }

    Map<InfoNeed, List<ModelScore>> featureMap(List<InfoNeed> queries, String baseModel, int n, Set<String> candidateModels) throws IOException {

        Map<InfoNeed, List<ModelScore>> featureMap = new HashMap<>(queries.size());

        for (InfoNeed need : queries) {
            Map<String, Double> base = documentRelevanceScores(baseModel, need, n);

            List<ModelScore> list = new ArrayList<>();

            for (String candidateModel : candidateModels) {
                Map<String, Double> candidate = documentRelevanceScores(candidateModel, need, n);

                Set<String> intersection = intersection(base, candidate);

                if (intersection.isEmpty()) {
                    //System.out.println("intersection is empty");
                    list.add(new ModelScore(candidateModel, Double.MAX_VALUE));
                    continue;
                }

                //System.out.println(intersection.size());

                // calculate divergence (KL and JS) over the data subset where both P and Q are non zero.
                // The K-L divergence is only defined if P and Q both sum to 1

                double[] rb = normalizeScores(intersection, base, c);

                double[] rc = normalizeScores(intersection, candidate, c);

                //System.out.println(Arrays.toString(rb));
                //System.out.println(Arrays.toString(rc));
                //System.out.println("Kullback-Leiber (KL) = " + kullbackLeiber(rb, rc));
                //System.out.println("Jensen-Shannon (JS) = " + jensenShannon(rb, rc));

                double divergence = kullbackLeiber(rb, rc);

                if (Double.isNaN(divergence)) {
                    System.out.println(base);
                    System.out.println(candidate);
                    System.out.println(intersection);
                    System.out.println(Arrays.toString(rb));
                    System.out.println(Arrays.toString(rc));
                    System.out.println("===============");
                }
                list.add(new ModelScore(candidateModel, divergence));
            }

            featureMap.put(need, list);
        }


        return featureMap;
    }

    private Map<Integer, Double> emptyDoubleMap() {
        Map<Integer, Double> doubleMap = new HashMap<>();
        for (int k = 1; k <= rangeK; k++) {
            doubleMap.put(k, 0.0);
        }
        return doubleMap;
    }

    private double neighbours(List<NeedDistance> needDistanceList, String candidateModel, int k) {
        double sum = 0.0;
        for (int i = 0; i < k; i++)
            sum += evaluator.score(needDistanceList.get(i).need, candidateModel);
        return sum / k;
    }

    Map<Integer, Double> process(List<InfoNeed> trainingQueries, List<InfoNeed> testQueries, int n) throws IOException {


        Map<InfoNeed, List<ModelScore>> trainingFeatureMap = featureMap(trainingQueries, baseModel, n, candidateModels);


        Map<InfoNeed, List<ModelScore>> testFeatureMap = featureMap(testQueries, baseModel, n, candidateModels);


        Map<Integer, Double> doubleMap = emptyDoubleMap();


        for (InfoNeed testQuery : testQueries) {

            Map<String, Double> test = list2map(testFeatureMap.get(testQuery));

            Map<Integer, List<ModelScore>> map = new HashMap<>();
            for (int k = 1; k <= rangeK; k++) {
                map.put(k, new ArrayList<>(candidateModels.size()));
            }

            for (String candidateModel : candidateModels) {

                final double testDouble = test.get(candidateModel);

                List<NeedDistance> needDistanceList = new ArrayList<>(trainingQueries.size());

                for (InfoNeed trainingQuery : trainingQueries) {

                    Map<String, Double> train = list2map(trainingFeatureMap.get(trainingQuery));

                    final double trainDouble = train.get(candidateModel);

                    final double distance = Math.abs(trainDouble - testDouble);

                    needDistanceList.add(new NeedDistance(trainingQuery, distance));

                }

                Collections.sort(needDistanceList);

                for (int k = 1; k <= rangeK; k++) {
                    double sum = neighbours(needDistanceList, candidateModel, k);
                    map.get(k).add(new ModelScore(candidateModel, sum));
                }

            }

            for (int k = 1; k <= rangeK; k++) {

                List<ModelScore> modelScoreList = map.get(k);
                Collections.sort(modelScoreList);

                double mean = doubleMap.get(k);

                final String predictedModel = modelScoreList.get(0).model;
                mean += evaluator.score(testQuery, predictedModel);

                doubleMap.put(k, mean);
            }

        }

        return divideElementsBy(doubleMap, testQueries.size());
    }

    private Map<Integer, Double> divideElementsBy(Map<Integer, Double> doubleMap, int n) {
        for (int k = 1; k <= rangeK; k++) {
            double mean = doubleMap.get(k);
            doubleMap.put(k, mean / n);
        }

        return doubleMap;
    }

    public static Map<String, Double> list2map(List<ModelScore> list) {
        Map<String, Double> map = new HashMap<>(list.size());

        for (ModelScore modelScore : list)
            map.put(modelScore.model, modelScore.score);

        return map;
    }

    String baseModel = null;
    Set<String> candidateModels = null;

    public void setEvaluator(Evaluator evaluator) {

        final Set<InfoNeed> allZero = evaluator.getAllZero();
        final Set<InfoNeed> allSame = evaluator.getAllSame();

        if (!allZero.isEmpty() || !allSame.isEmpty())
            throw new RuntimeException("Evaluator must be instantiated with residual information needs!");

        this.evaluator = evaluator;

        Set<String> models = evaluator.getModelSet();

        for (String model : models)
            if (model.startsWith("BM25")) {
                baseModel = model;
                break;
            }

        if (baseModel == null) throw new RuntimeException("cannot find bm25 in the model set " + models);

        candidateModels = new HashSet<>(models);
        candidateModels.remove(baseModel);
    }

    public ModelScore evaluate(Evaluator evaluator) throws IOException {

        setEvaluator(evaluator);

        List<InfoNeed> residualNeeds = evaluator.getNeeds();


        final Track[] tracks = dataSet.tracks();

        double maxMean = Double.NEGATIVE_INFINITY;

        int K = -1;
        int N = -1;

        if (tracks.length == 1) {
            System.out.println("The current train/test split mechanism requires more than one tracks.");
            return new ModelScore("LTS (k=" + K + ", n=" + N + ")", Double.NaN);
        }

        for (int n = 20; n <= 1000; n += 10) {

            Map<Integer, Double> mean = emptyDoubleMap();

            for (Track track : tracks) {

                List<InfoNeed> trainingQueries = trainingQueries(residualNeeds, track);
                List<InfoNeed> testQueries = testQueries(residualNeeds, track);

                System.out.println("n=" + n + " training set size : " + trainingQueries.size() + " test set size : " + testQueries.size());


                if (trainingQueries.size() + testQueries.size() != residualNeeds.size())
                    throw new RuntimeException("expect the unexpected");

                Map<Integer, Double> doubleMap = process(trainingQueries, testQueries, n);

                for (int k = 1; k <= rangeK; k++) {
                    double d = mean.get(k);
                    d += doubleMap.get(k);
                    mean.put(k, d);
                }
            }

            mean = divideElementsBy(mean, tracks.length);

            double localMax = Double.NEGATIVE_INFINITY;
            int localK = -1;
            for (int k = 1; k <= rangeK; k++) {

                if (mean.get(k) > localMax) {
                    localK = k;
                    localMax = mean.get(k);
                }

            }

            if (localMax > maxMean) {
                K = localK;
                N = n;
                maxMean = localMax;
            }
        }

        if (K == -1 || N == -1) throw new RuntimeException("expect the unexpected");

        return new ModelScore("LTS (k=" + K + ", n=" + N + ")", maxMean);
    }
}

