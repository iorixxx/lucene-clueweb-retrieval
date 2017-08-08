package edu.anadolu.knn;

import com.google.common.collect.*;
import edu.anadolu.Decorator;
import edu.anadolu.cmdline.EvaluatorTool;
import edu.anadolu.cmdline.SpamEvalTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.exp.FullFactorial;
import edu.anadolu.freq.Freq;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * k-Nearest Neighbors (k-NN) classification algorithm.
 * A query is classified by a majority vote of its neighbors,
 * with the query being assigned to the term-weighting model most common among its k nearest neighbors.
 */
public class KNNTool extends EvaluatorTool {

    //   @Option(name = "-zero", required = false, usage = "Include zero column")
    //   private boolean zero = false;


    public static List<ChiBase> chis() {

        List<ChiBase> chis = new ArrayList<>();

        for (boolean divide : new boolean[]{false})
            for (boolean cdf : new boolean[]{false})

            {

                if (cdf && divide) continue;

                // Take account into zero: number of documents that does not contain the term (idf)


                chis.add(new ChiSquare(divide, cdf));


                // Does not consider zero: number of documents that does not contain the term (idf)

                /**
                 chis.add(new XChi(divide, cdf));

                 if (!cdf) {
                 chis.add(new KullbackLeiber(divide));
                 chis.add(new JensenShannon(divide));
                 }
                 **/

            }

        return Collections.unmodifiableList(chis);
    }

    public static List<QuerySimilarity> querySimilarities(boolean zero) {

        List<QuerySimilarity> querySimilarities = new ArrayList<>();

        for (ChiBase chi : chis()) {
            // querySimilarities.add(new GeoDFQuerySimilarity(chi, zero));
            // querySimilarities.add(new DFGeoQuerySimilarity(chi, zero));
            //ignore ADF
            //  querySimilarities.add(new AverageDFQuerySimilarity(chi, zero));
            if (!zero)
                querySimilarities.add(new DFAverageQuerySimilarity(chi, zero));
            for (CartesianQueryTermSimilarity.Aggregation agg : CartesianQueryTermSimilarity.Aggregation.values())
                for (CartesianQueryTermSimilarity.Way way :
                        new CartesianQueryTermSimilarity.Way[]
                                {
                                        CartesianQueryTermSimilarity.Way.m
                                }) {
                    querySimilarities.add(new CartesianQueryTermSimilarity(chi, zero, agg, way));
                    //  querySimilarities.add(new DiscountCartesianSimilarity(chi, zero, agg, way));
                    // querySimilarities.add(new UltimateCartesianSimilarity(chi, zero, agg, way));
                }

            //  querySimilarities.add(new XCartesianSimilarity(chi, zero, XCartesianSimilarity.Aggregation.Euclid));
            //  querySimilarities.add(new XCartesianSimilarity(chi, zero, XCartesianSimilarity.Aggregation.Sum));
        }

        return Collections.unmodifiableList(querySimilarities);
    }


    public static List<QuerySimilarity> querySimilarities() {

        List<QuerySimilarity> querySimilarities = new ArrayList<>();

        querySimilarities.addAll(querySimilarities(true));
        //querySimilarities.addAll(querySimilarities(false));

        return Collections.unmodifiableList(querySimilarities);
    }


    Voter[] voters(Evaluator evaluator, Map<InfoNeed, List<ModelScore>> sortedPerformanceMap) {

        return new Voter[]{
                new MajorityVoter(sortedPerformanceMap),
                new MeanVoter(evaluator)
        };

    }


    @Override
    public String getShortDescription() {
        return "KNN Utility";
    }


    public final double[] scoreArray(int size, Evaluator evaluator, String model, Fold[] folds) {

        double[] scores = new double[size];
        int c = 0;

        if (c != size) throw new RuntimeException("counter :" + c + " does not match size : " + size);
        return scores;
    }

    public final double[] oracleMinScoreArray(Evaluator evaluator, List<InfoNeed> residualTFDAwareNeeds, Fold[] folds) {

        Map<InfoNeed, Double> oracleMinScoreMap = evaluator.oracleMinScoreMap(residualTFDAwareNeeds);
        double[] oracleMinScores = new double[residualTFDAwareNeeds.size()];
        int c = 0;
        for (Fold fold : folds)
            for (InfoNeed testQuery : fold.testQueries) {
                oracleMinScores[c++] = oracleMinScoreMap.get(testQuery);
            }

        if (c != residualTFDAwareNeeds.size())
            throw new RuntimeException("counter :" + c + " does not match size : " + residualTFDAwareNeeds.size());

        return oracleMinScores;
    }

    public final double[] oracleMaxScoreArray(Evaluator evaluator, int size, Fold[] folds) {

        double[] oracleMaxScores = new double[size];
        int c = 0;
        for (Fold fold : folds)
            for (InfoNeed testQuery : fold.testQueries) {


                double max = Double.NEGATIVE_INFINITY;

                for (String model : evaluator.getModelSet()) {
                    double score = evaluator.score(testQuery, model);
                    if (score > max)
                        max = score;
                }

                oracleMaxScores[c++] = max;


            }

        if (c != size)
            throw new RuntimeException("counter :" + c + " does not match size : " + size);

        return oracleMaxScores;
    }

    public double[] randomXScoreArray(Evaluator evaluator, int size, Fold[] folds) {

        double[] scores = new double[size];
        List<String> modelSet = new ArrayList<>(evaluator.getModelSet());

        /**
         * How many times a model winner?
         * That much addition is done for the model
         */
        List<String> localModels = new ArrayList<>(size);

        for (String model : modelSet) {

            // a model may not exist in best model map, e.g. winner for zero queries
            if (!evaluator.bestModelMap().containsKey(model)) continue;

            int count = evaluator.bestModelMap().get(model).size();
            for (int i = 0; i < count; i++)
                localModels.add(model);
        }

        try (DataInputStream is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")))) {

            for (int i = localModels.size(); i < size; i++) {
                int index = Math.abs(is.readInt()) % modelSet.size();
                localModels.add(modelSet.get(index));
            }

            int c = 0;
            for (Fold fold : folds)
                for (InfoNeed testQuery : fold.testQueries) {

                    scores[c] = evaluator.score(testQuery, localModels.remove(Math.abs(is.readInt()) % localModels.size()));
                    c++;
                }

            if (c != size)
                throw new RuntimeException("counter :" + c + " does not match size : " + size);

        } catch (IOException ioe) {
            throw new RuntimeException("getting random integers from /dev/urandom", ioe);
        }


        return scores;
    }

    public final double[] randomScoreArray(Evaluator evaluator, int size, Fold[] folds) {

        double[] randomScores = new double[size];

        List<String> modelSet = new ArrayList<>(evaluator.getModelSet());

        int c = 0;
        try (DataInputStream is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")))) {


            for (Fold fold : folds)
                for (InfoNeed testQuery : fold.testQueries) {

                    int index = Math.abs(is.readInt()) % modelSet.size();
                    randomScores[c++] = evaluator.score(testQuery, modelSet.get(index));
                }

        } catch (IOException ioe) {
            throw new RuntimeException("getting random integers from /dev/urandom", ioe);
        }

        if (c != size)
            throw new RuntimeException("counter :" + c + " does not match size : " + size);

        return randomScores;

    }


    protected Workbook workbook;

    private static final int[] percentages = {
            10,
            15,
            20,
            25,
            30,
            35,
            40,
            45,
            50,
            55,
            60,
            65,
            70,
            75,
            80,
            85,
            90};

    private static ArrayList<Integer> kValues(int minK) {

        ArrayList<Integer> kValues = new ArrayList<>(percentages.length);

        for (int i = 0; i < percentages.length; i++)
            kValues.add(i, minK * percentages[i] / 100);

        return kValues;
    }


    private final TStats tStats = new TStats();

    double t(final double[] x, final double[] y) {
        double t = tStats.tStats(x, y);

        if (Double.isInfinite(t)) {
            throw new RuntimeException("Infinite : " + Arrays.toString(x) + "\n" + Arrays.toString(y));
        }

        if (Double.isNaN(t)) {
            return 0.0;
        } else
            return t;

    }

    double z(final double[] x, final double[] y) {
        return tStats.z(x, y);
    }


    public void addZRisk2Sheet(Sheet RxTxP, int r, int c) {

        RxTxP.getRow(0).createCell(c + 2, CellType.STRING).setCellValue("zRisk");
        RxTxP.getRow(0).createCell(c + 3, CellType.STRING).setCellValue("geoRisk");

        final double[] rowSum = new double[r + 1];
        Arrays.fill(rowSum, 0.0);

        final double[] columnSum = new double[c + 2];
        Arrays.fill(columnSum, 0.0);

        double N = 0.0;

        for (int i = 1; i < r + 1; i++) {

            Row row = RxTxP.getRow(i);

            for (int j = 2; j < c + 2; j++) {

                if (row.getCell(j) == null) {
                    throw new RuntimeException("encountered null cell i=" + i + " j=" + j + " during geoZrisk addition");
                }
                final double cellValue = row.getCell(j).getNumericCellValue();

                rowSum[i] += cellValue;
                columnSum[j] += cellValue;
                N += cellValue;

                //System.out.print(String.format("%.4f", cellValue) + " ");
            }

            //System.out.println();
        }

        //System.out.println(Arrays.toString(rowSum));
        //System.out.println(Arrays.toString(columnSum));
        //System.out.println(N);

        for (int i = 1; i < r + 1; i++) {

            Row row = RxTxP.getRow(i);
            double zRisk = 0.0;
            double si = 0.0;

            for (int j = 2; j < c + 2; j++) {

                final double cellValue = row.getCell(j).getNumericCellValue();
                final double e = rowSum[i] * columnSum[j] / N;
                si += cellValue;

                if (e == 0.0) continue;

                zRisk += (cellValue - e) / Math.sqrt(e);

            }

            final double geoRisk = Math.sqrt(si / c * normalDistribution.cumulativeProbability(zRisk / c));

            row.createCell(c + 2, CellType.NUMERIC).setCellValue(zRisk);
            row.createCell(c + 3, CellType.NUMERIC).setCellValue(geoRisk);

        }

    }

    final NormalDistribution normalDistribution = new NormalDistribution();

    /**
     * Adds following header to 0th row:
     * Percentages	10P	15P	20P	25P	30P	35P	40P	45P	50P	55P	60P	65P	70P	75P	80P	85P	90P
     *
     * @param sheet sheet
     */
    static void addPercentagesHeader(Sheet sheet) {
        Row r0 = sheet.createRow(0);
        r0.createCell(1).setCellValue("Percentages");
        int counter = 1;
        for (int p : percentages) {
            r0.createCell(++counter).setCellValue(p + "P");
        }
    }

    /**
     * Normalize excel data using cumulative distribution function of the standard normal distribution
     *
     * @param sheet input sheet
     * @param r     number of rows
     * @param c     number of columns
     */
    public Sheet normalize(Sheet sheet, int r, int c) {

        Sheet target = workbook.createSheet(sheet.getSheetName() + "-Phi");
        addPercentagesHeader(target);

        for (int i = 1; i < r + 1; i++) {

            Row row = sheet.getRow(i);

            Row targetRow = target.createRow(i);
            targetRow.createCell(1, CellType.STRING).setCellValue(row.getCell(1).getStringCellValue());

            for (int j = 2; j < c + 2; j++) {

                double cellValue;
                try {
                    cellValue = row.getCell(j).getNumericCellValue();
                } catch (java.lang.IllegalStateException exp) {

                    System.out.println("error get numeric cell value i= " + i + " j=" + j);
                    DataFormatter formatter = new DataFormatter(Locale.US); //creating formatter using the default locale
                    Cell cell = row.getCell(j);
                    String string = formatter.formatCellValue(cell); //Returns the for
                    System.out.println("string value = " + string);
                    continue;
                }


                final double targetValue = normalDistribution.cumulativeProbability(cellValue);

                targetRow.createCell(j, CellType.NUMERIC).setCellValue(targetValue);

            }

        }
        return target;
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

        Path excelPath = dataset.collectionPath().resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        Path excelFile = excelPath.resolve(collection.toString() + tag + measure + op.toUpperCase(Locale.ENGLISH) + ".xlsx");


        workbook = new XSSFWorkbook();


        System.out.println("START!");

        final String evalDirectory;

        if (Collection.GOV2.equals(collection) || Collection.MC.equals(collection) || Collection.ROB04.equals(collection)) {
            evalDirectory = "evals";
        } else {
            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, op);
            evalDirectory = bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }

        System.out.println("Instantiating Evaluator with evaluation directory of " + evalDirectory);

        Evaluator evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory, op);

        final Set<String> modelSet = evaluator.getModelSet();
        final Set<InfoNeed> allZero = evaluator.getAllZero();
        final Set<InfoNeed> allSame = evaluator.getAllSame();

        System.out.println(measure + tag + op.toUpperCase(Locale.US) + " zero=" + allZero.size() + " allSame=" + allSame.size());

        List<InfoNeed> infoNeedList = new ArrayList<>();

        for (Track track : dataset.tracks()) {
            infoNeedList.addAll(track.getTopics());
        }

        List<InfoNeed> residualNeeds = new ArrayList<>(infoNeedList);
        residualNeeds.removeAll(allSame);
        residualNeeds.removeAll(allZero);

        final ModelScore oracle = evaluator.oracleMin(residualNeeds);

        List<ModelScore> modelMeans = evaluator.averageForAllModels(residualNeeds);
        Collections.sort(modelMeans);
        ModelScore bestSingle = modelMeans.get(0);


        System.out.println("initial needs size = " + infoNeedList.size());

        System.out.println("needs size after allZeros and allSame excluded = " + residualNeeds.size());

        System.out.println("Model Means : ");

        if (residualNeeds.size() < 100)
            System.out.print("ALL(" + residualNeeds.size() + ") \t");
        else
            System.out.print("ALL(" + residualNeeds.size() + ")\t");

        for (ModelScore modelScore : modelMeans)
            System.out.print(modelScore.model + "(" + String.format("%.5f", modelScore.score) + ")\t");

        System.out.println();

        System.out.println("Models : " + modelSet);

        System.out.println("OracleMin and Best Single : " + oracle + "\t" + bestSingle);

        final Map<InfoNeed, List<ModelScore>> sortedPerformanceMap = evaluator.getSortedPerformanceMap(residualNeeds);

        //final LinkedHashMap<InfoNeed, LinkedHashSet<String>> multiLabelMap = evaluator.multiLabelMap();

        final Map<InfoNeed, Set<String>> singleLabelMap = evaluator.singleLabelMap();

        final Decorator decorator = new Decorator(dataset, tag, Freq.Rel);


        Fold[] folds = folds(dataset, residualNeeds);

        System.out.println("minK = " + minK);

        final double[] oracleMaxScores = oracleMaxScoreArray(evaluator, residualNeeds.size(), folds);
        System.out.println("Oracle Max\t" + StatUtils.mean(oracleMaxScores));

        final double[] oracleMinScores = oracleMinScoreArray(evaluator, residualNeeds, folds);
        System.out.println(oracle + "\t" + StatUtils.mean(oracleMinScores));

        final double[] bestSingleScores = scoreArray(residualNeeds.size(), evaluator, bestSingle.model, folds);
        System.out.println(bestSingle + "\t" + StatUtils.mean(bestSingleScores));

        List<Prediction> bestSingleList = new ArrayList<>();

        for (Fold fold : folds)
            for (InfoNeed testQuery : fold.testQueries) {
                Prediction prediction = new Prediction(testQuery, bestSingle.model, evaluator.score(testQuery, bestSingle.model));
                bestSingleList.add(prediction);
            }

        Solution bestSingleSolution = new Solution(bestSingleList, -1);
        evaluator.calculateAccuracy(bestSingleSolution);

        System.out.println("Accuracy of best single " + "\t" + bestSingleSolution.sigma1);


        ArrayList<Integer> kValues = kValues(minK);

        for (int p = 0; p < percentages.length; p++) {

            Sheet sheet = workbook.createSheet();
            workbook.setSheetName(p, "k" + Integer.toString(percentages[p]) + "P");

            Row r0 = sheet.createRow(0);
            r0.createCell(1).setCellValue("Run Name");

            Row r1 = sheet.createRow(1);
            r1.createCell(1).setCellValue("Oracle Max");

            Row r2 = sheet.createRow(2);
            r2.createCell(1).setCellValue("Oracle Min");

            Row r3 = sheet.createRow(3);
            r3.createCell(1).setCellValue("Best");


//            sheet.addCell(new Label(1, 0, "Run Name"));
//            sheet.addCell(new Label(1, 1, "Oracle Max"));
//            sheet.addCell(new Label(1, 2, "Oracle Min"));
//            sheet.addCell(new Label(1, 3, "Best"));


            r0.createCell(2).setCellValue("Run");
            r1.createCell(2).setCellValue(-2);
            r2.createCell(2).setCellValue(-1);
            r3.createCell(2).setCellValue(0);

//            sheet.addCell(new Label(2, 0, "Run"));
//            sheet.addCell(new Number(2, 1, -2));
//            sheet.addCell(new Number(2, 2, -1));
//            sheet.addCell(new Number(2, 3, 0));


            int c = 2;
            int counter = 0;
            for (Fold fold : folds)
                for (final InfoNeed testQuery : fold.testQueries) {

                    c++;
                    int qID = testQuery.id();

                    r0.createCell(c).setCellValue("T" + qID);
                    r1.createCell(c).setCellValue(oracleMaxScores[counter]);
                    r2.createCell(c).setCellValue(oracleMinScores[counter]);
                    r3.createCell(c).setCellValue(bestSingleScores[counter]);


//                    sheet.addCell(new Label(c, 0, "T" + qID);
//
//                    sheet.addCell(new Number(c, 1, oracleMaxScores[counter]));
//                    sheet.addCell(new Number(c, 2, oracleMinScores[counter]));
//                    sheet.addCell(new Number(c, 3, bestSingleScores[counter]));


                    counter++;

                }
        }


        /**
         * Insert Run X Topic Matrix as a separate sheet
         */
        Sheet RxT = workbook.createSheet("RxT");

        Row r0 = RxT.createRow(0);
        r0.createCell(1).setCellValue("Model");

        //  RxT.addCell(new Label(1, 0, "Model"));
        int counter = 0;
        for (String model : modelSet) {

            Row r = RxT.createRow(++counter);
            r.createCell(1).setCellValue(model);

            //RxT.addCell(new Label(1,++counter , model));
        }

        RxT.createRow(++counter).createCell(1).setCellValue("OracleMin");
        RxT.createRow(++counter).createCell(1).setCellValue("OracleMax");


        int c = 1;

        for (Fold fold : folds)
            for (final InfoNeed testQuery : fold.testQueries) {

                c++;
                int qID = testQuery.id();

                r0.createCell(c).setCellValue("T" + qID);
                //RxT.addCell(new Label(c, 0, "T" + qID));

                counter = 0;
                for (String model : modelSet) {
                    // RxT.addCell(new Number(c, ++counter, evaluator.score(testQuery, model)));
                    Row r = RxT.getRow(++counter);
                    r.createCell(c).setCellValue(evaluator.score(testQuery, model));
                }

                RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(oracleMinScores[c - 2]);
                RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(oracleMaxScores[c - 2]);

            }


        /**
         * T-statistics separate sheet, Run X Percentages for Best Single Run
         */
        Sheet RxPxT_B = workbook.createSheet("RxPxT-Best");
        addPercentagesHeader(RxPxT_B);


        /**
         * Wilcoxon statistics separate sheet, Run X Percentages for Best Single Run
         */
        Sheet RxPxW_B = workbook.createSheet("RxPxW-Best");
        addPercentagesHeader(RxPxW_B);


        /**
         * T-statistics separate sheet, Run X Percentages for Oracle Min
         */
        Sheet RxPxT_Omin = workbook.createSheet("RxPxT-Omin");
        addPercentagesHeader(RxPxT_Omin);

        /**
         * Wilcoxon statistics separate sheet, Run X Percentages for Oracle Min
         */
        Sheet RxPxW_Omin = workbook.createSheet("RxPxW-Omin");
        addPercentagesHeader(RxPxW_Omin);

        /**
         * T-statistics separate sheet, Run X Percentages for Oracle Max
         */
        Sheet RxPxT_Omax = workbook.createSheet("RxPxT-Omax");
        addPercentagesHeader(RxPxT_Omax);

        /**
         * Wilcoxon statistics separate sheet, Run X Percentages for Oracle Max
         */
        Sheet RxPxW_Omax = workbook.createSheet("RxPxW-Omax");
        addPercentagesHeader(RxPxW_Omax);

        /**
         * RxTxP(meanERR@20): Cells contain the mean over all topic for each Percentile
         */
        Sheet RxTxP = workbook.createSheet("RxTxP");
        addPercentagesHeader(RxTxP);

        Sheet RxAxP = workbook.createSheet("RxAxP");
        addPercentagesHeader(RxAxP);

        final List<Solution> solutionList = new ArrayList<>();

        final Map<String, List<Pair>> failedTopics = new LinkedHashMap<>();


        /**
         * Main loop, all 72 runs
         */
        int row = 3;
        int runCounter = 0;
        for (final Voter voter : voters(evaluator, sortedPerformanceMap))
            for (final QuerySimilarity querySimilarity : querySimilarities()) {

                row++;
                runCounter++;
                final String key = voter.toString() + "," + querySimilarity.toString();


                for (int p = 0; p < percentages.length; p++) {
                    Sheet sheet = workbook.getSheetAt(p);
                    Row r = sheet.createRow(row);
                    r.createCell(1).setCellValue(key);
                    r.createCell(2).setCellValue(runCounter);
//                    Label label = new Label(1, row, key);
//                    Number number = new Number(2, row, runCounter);
//                    sheet.addCell(label);
//                    sheet.addCell(number);
                }


                /**
                 * Keys are k values 0f k-NN, values are list of predicted models
                 */
                Map<Integer, List<Prediction>> modelScoreMap = new LinkedHashMap<>();

                int column = 2;

                for (Fold fold : folds)
                    for (final TFDAwareNeed testQuery : decorator.residualTFDAwareNeeds(fold.testQueries)) {

                        column++;

                        // System.out.println(testQuery.id());


                        List<Pair> list = new ArrayList<>();
                        for (TFDAwareNeed trainingQuery : decorator.residualTFDAwareNeeds(fold.trainingQueries)) {

                            double sim = querySimilarity.score(testQuery, trainingQuery);
                            if (Double.isInfinite(sim) || Double.isNaN(sim)) {
                                System.err.println(querySimilarity.toString());
                                System.err.print("sim " + sim);
                                System.err.println(" test " + testQuery);
                                System.err.println("train" + trainingQuery);
                            }
                            list.add(new Pair(trainingQuery, sim));
                        }

                        try {
                            Collections.sort(list);
                        } catch (Exception e) {
                            System.err.print(collection + " " + tag + " " + measure + " " + key);
                        }

                        for (int k = 1; k < minK; k++) {

                            String predictedModel = voter.vote(list.subList(0, k));
                            double predictedScore = evaluator.score(testQuery, predictedModel);

                            int index = kValues.indexOf(k);
                            if (index != -1) {
                                Sheet excelSheet = workbook.getSheetAt(index);
                                Row r = excelSheet.getRow(row);
                                r.createCell(column).setCellValue(predictedScore);
                                //excelSheet.addCell(new Number(column, row, predictedScore));
                            }

                            Prediction predicted = new Prediction(testQuery, predictedModel, predictedScore);

                            if (modelScoreMap.containsKey(k))
                                modelScoreMap.get(k).add(predicted);
                            else {
                                List<Prediction> modelScoreList = new ArrayList<>();
                                modelScoreList.add(predicted);
                                modelScoreMap.put(k, modelScoreList);
                            }

                        }

                    }


                /**
                 * t statistics excel sheet
                 */
                // .addCell(new Label(1, runCounter, key));
                RxPxT_B.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxPxW_B.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxPxT_Omin.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxPxT_Omax.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxPxW_Omin.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxPxW_Omax.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxTxP.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);
                RxAxP.createRow(runCounter).createCell(1, CellType.STRING).setCellValue(key);

                int cc = 1;
                for (int k = 1; k < minK; k++) {

                    if (modelScoreMap.get(k).size() != residualNeeds.size())
                        throw new RuntimeException("list sizes are not equal!");

                    int index = kValues.indexOf(k);
                    if (index == -1) continue;
                    Solution solution = new Solution(modelScoreMap.get(k), k);
                    evaluator.calculateAccuracy(solution);

                    cc++;


                    RxPxT_B.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(t(bestSingleScores, solution.scores));
                    RxPxT_Omin.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(t(oracleMinScores, solution.scores));
                    RxPxT_Omax.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(t(oracleMaxScores, solution.scores));

                    RxPxW_B.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(z(bestSingleScores, solution.scores));
                    RxPxW_Omin.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(z(oracleMinScores, solution.scores));
                    RxPxW_Omax.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(z(oracleMaxScores, solution.scores));

                    RxTxP.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(solution.getMean());
                    RxAxP.getRow(runCounter).createCell(cc, CellType.NUMERIC).setCellValue(solution.sigma1);
                }


                for (int k = 1; k < minK; k++) {

                    if (modelScoreMap.get(k).size() != residualNeeds.size())
                        throw new RuntimeException("list sizes are not equal!");

                    Solution solution = new Solution(modelScoreMap.get(k), k);
                    evaluator.calculateAccuracy(solution);

                    if (solution.getMean() > bestSingleSolution.getMean() && solution.sigma1 > bestSingleSolution.sigma1) {

                        solution.calculateImprovement(bestSingle, oracle);


                        String runLabel = String.format("%s\t%s\tk=%d\t%s=%.5f\tmultiAcc=%.2f", voter.toString(), querySimilarity.toString(), k, evaluator.metric().toString(), solution.getMean(), solution.sigma1);
                        solution.setRunLabel(runLabel);

                        solution.setKey(key);
                        solutionList.add(solution);
                    }
                }


                /**
                 *  poorly classified topics
                 */
                int oneBestK = kValues.get(2); // 20P


                List<Prediction> results = modelScoreMap.get(oneBestK);

                if (results.size() != residualNeeds.size())
                    throw new RuntimeException("list sizes are not equal!");

                int correct = 0;
                List<Pair> failedTopicList = new ArrayList<>();
                for (Prediction prediction : results) {

                    InfoNeed testQuery = prediction.testQuery;

                    if (singleLabelMap.get(testQuery).contains(prediction.predictedModel))
                        correct++;
                    else {
                        if (evaluator.score(testQuery, prediction.predictedModel) != prediction.predictedScore)
                            throw new RuntimeException("predicted scores are different for the test query : " + testQuery);
                        double howBadlyFailed = evaluator.bestScore(testQuery) - evaluator.score(testQuery, prediction.predictedModel);
                        Pair pair = new Pair(testQuery, howBadlyFailed);
                        pair.predictedModel = prediction.predictedModel;
                        failedTopicList.add(pair);
                    }
                }
                double classificationAccuracy = (double) correct / residualNeeds.size() * 100.0;

                failedTopics.put(key, failedTopicList);


            }

        addZRisk2Sheet(RxTxP, runCounter, kValues.size());
        addZRisk2Sheet(RxAxP, runCounter, kValues.size());

        Sheet RxPxT_B_Phi = normalize(RxPxT_B, runCounter, kValues.size());
        normalize(RxPxT_Omin, runCounter, kValues.size());
        normalize(RxPxT_Omax, runCounter, kValues.size());

        Sheet RxPxW_B_Phi = normalize(RxPxW_B, runCounter, kValues.size());
        normalize(RxPxW_Omin, runCounter, kValues.size());
        normalize(RxPxW_Omax, runCounter, kValues.size());

        addZRisk2Sheet(RxPxT_B_Phi, runCounter, kValues.size());
        addZRisk2Sheet(RxPxW_B_Phi, runCounter, kValues.size());

        Multiset<String> multiset = HashMultiset.create();

        ListMultimap<String, Solution> multimap = ArrayListMultimap.create();

        for (Solution solution : solutionList) {

//            double wP = wilcoxonSignedRankTest.wilcoxonSignedRankTest(solution.scores, bestSingleScores, false);
//            double wPP = wilcoxonSignedRankTest.wilcoxonSignedRankTest(bestSingleScores, solution.scores, false);
//
//            System.err.println(wP + "\t" + wPP);

            //double tP = new TTest().pairedTTest(solution.scores, bestSingleScores) / 2.0d;


            // if (tP < 0.05 && wP < 0.05)
            {
                //System.out.println(String.format("*** %.2f\t%s", solution.improvement, solution.runLabel));
                multiset.add(solution.key);
                multimap.put(solution.key, solution);

            } //else System.out.println(String.format("tP = %.5f wP = %.5f\t", tP, wP));

            // System.out.println(String.format("%.2f\t%s", solution.improvement, solution.runLabel));
        }


        Map<String, Integer> runNameMap = runNameMap(false);
        Map<Integer, String> reverse = revert(runNameMap);
        Map<String, Double> significantResults = new HashMap<>();

        List<Solution> subList = new ArrayList<>();

        for (String key : Multisets.copyHighestCountFirst(multiset).elementSet()) {

            List<Solution> solutions = multimap.get(key);
            Collections.sort(solutions);
            Solution bestSolution = solutions.get(0);
            subList.add(bestSolution);
            double myMetric = (double) multiset.count(key) / minK * 100d;
            System.out.println(runNameMap.get(key) + "\t" + key + "\t" + multiset.count(key) + "\tk=" + bestSolution.k + "\t" + String.format("%.2f", myMetric) + "\t" + String.format("%.5f", bestSolution.getMean()) + "\t" + String.format("%.2f", bestSolution.sigma1));

            significantResults.put(key, myMetric);
        }

        System.out.println("END!");
        System.out.println("Result sorted by best sigma1");

        Collections.sort(subList, new Comparator<Solution>() {
            @Override
            public int compare(Solution o1, Solution o2) {
                return (int) Math.signum(o2.sigma1 - o1.sigma1);
            }
        });
        for (Solution solution : subList)
            System.out.println(runNameMap.get(solution.key) + "\t" + solution.key + "\t" + "\tk=" + solution.k + "\t" + String.format("%.5f", solution.getMean()) + "\t" + String.format("%.2f", solution.sigma1));


        /**
         for (Map.Entry<String, List<Pair>> entry : failedTopics.entrySet()) {
         System.out.println(entry.getKey() + " (" + entry.getValue().size() + "/" + residualTFDAwareNeeds.size() + ") " + " : ");
         List<Pair> pairList = entry.getValue();
         Collections.sort(pairList);
         Collections.reverse(pairList);
         for (Pair pair : pairList) {
         System.out.println(pair.infoNeed.id() + "(" + String.format("%.4f", pair.similarity) + ") " + pair.predictedModel);
         System.out.println(evaluator.sortedTopicModel(pair.infoNeed));
         System.out.println("-------------------------");
         }

         System.out.println();
         }

         for (int i = 1; i <= runNameMap.size(); i++) {
         String key = reverse.get(i);
         if (significantResults.containsKey(key))
         System.out.println(i + "\t" + key + "\t" + String.format("%.2f", significantResults.get(key)));
         else
         System.out.println(i + "\t" + key + "\t0.0");
         }
         **/
        workbook.write(Files.newOutputStream(excelFile));
        workbook.close();

    }

    int minK = Integer.MAX_VALUE;


    Fold[] foldsForOne(List<? extends InfoNeed> residualTFDAwareNeeds) {

        List<InfoNeed> shuffledNeeds = new ArrayList<>(residualTFDAwareNeeds);

        Collections.shuffle(shuffledNeeds);

        Fold[] folds = new Fold[4];

        List<List<InfoNeed>> parts = chopped(shuffledNeeds, 4);

        int i = 0;

        for (List<InfoNeed> testQueries : parts) {

            List<InfoNeed> trainingQueries = new ArrayList<>(shuffledNeeds);
            trainingQueries.removeAll(testQueries);

            if (trainingQueries.size() < minK)
                minK = trainingQueries.size();

            if (trainingQueries.size() + testQueries.size() != residualTFDAwareNeeds.size())
                throw new RuntimeException("expect the unexpected");

            folds[i++] = new Fold(trainingQueries, testQueries);
        }

        return folds;

    }

    // chops a list into L non-view sublists
    static <T> List<List<T>> chopped(List<T> list, final int L) {

        List<List<T>> parts = new ArrayList<>();

        final int chunkSize = list.size() / L;

        int start = 0;

        for (int i = 0; i < L; i++) {
            if (i == L - 1)
                parts.add(new ArrayList<>(list.subList(start, list.size())));
            else
                parts.add(new ArrayList<>(list.subList(start, start + chunkSize)));

            start = start + chunkSize;
        }
        return parts;
    }

    /**
     * Boolean factor with level order of TRUE FALSE
     *
     * @param flag factor
     * @return int array
     */
    static int[] boolArray(boolean flag) {
        int[] array = new int[2];
        Arrays.fill(array, 0);

        if (flag) {
            array[0] = 1;
        } else
            array[1] = 1;
        return array;

    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(percentages));
        System.out.println(kValues(50));

        runNameMap(true);

    }

    static Map<Integer, String> revert(Map<String, Integer> runNameMap) {

        Map<Integer, String> reverse = new HashMap<>();

        for (Map.Entry<String, Integer> entry : runNameMap.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }

        return reverse;

    }

    static Map<String, Integer> runNameMap(boolean print) {


        Map<String, Integer> runNameMap = new HashMap<>();


        List<String> voters = Arrays.asList("Mj", "Me");
        int[] votersArray = new int[voters.size()];

        List<Class<? extends QuerySimilarity>> simClasses = Arrays.asList(
                GeoDFQuerySimilarity.class,
                DFGeoQuerySimilarity.class,
                DFAverageQuerySimilarity.class,
                AverageDFQuerySimilarity.class,
                CartesianQueryTermSimilarity.class,
                DiscountCartesianSimilarity.class,
                UltimateCartesianSimilarity.class
        );

        int[] simsArray = new int[simClasses.size()];

        List<QuerySimilarity> similarities = querySimilarities();

        int counter = 0;
        for (final String voter : voters) {

            Arrays.fill(votersArray, 0);
            votersArray[voters.indexOf(voter)] = 1;

            for (final QuerySimilarity querySimilarity : similarities) {
                Arrays.fill(simsArray, 0);
                simsArray[simClasses.indexOf(querySimilarity.getClass())] = 1;

                ++counter;

                final String key = voter + "," + querySimilarity;

                runNameMap.put(key, counter);
                if (!print) continue;

                System.out.println("run" + counter + "\t" + key + "\t" + counter + "\t" +
                        FullFactorial.print(votersArray) + "\t" +
                        FullFactorial.print(simsArray) + "\t" +
                        (querySimilarity.zero() ? 1 : 0) + "\t" +
                        FullFactorial.print(boolArray(querySimilarity.chi().cdf)) + "\t" +
                        (querySimilarity.chi().divide ? 1 : 0) + "\t" +
                        (querySimilarity.chi() instanceof XChi ? 1 : 0)
                );

            }
        }

        return runNameMap;

    }


    Fold[] folds(DataSet dataset, List<InfoNeed> residualTFDAwareNeeds) {

        if (dataset.tracks().length == 1) {
            System.out.println("DataSet " + dataset.toString() + " has only one track " + dataset.tracks()[0].toString());
            return foldsForOne(residualTFDAwareNeeds);
        }

        Fold[] folds = new Fold[dataset.tracks().length];

        int i = 0;
        for (Track track : dataset.tracks()) {

            List<InfoNeed> trainingQueries = trainingQueries(residualTFDAwareNeeds, track);
            List<InfoNeed> testQueries = testQueries(residualTFDAwareNeeds, track);

            if (trainingQueries.size() < minK)
                minK = trainingQueries.size();

            if (trainingQueries.size() + testQueries.size() != residualTFDAwareNeeds.size())
                throw new RuntimeException("expect the unexpected");

            folds[i++] = new Fold(trainingQueries, testQueries);
        }

        return folds;
    }

    public static List<InfoNeed> trainingQueries(List<InfoNeed> residualNeeds, Track track) {
        return Collections.unmodifiableList(
                residualNeeds.stream()
                        .filter(need -> need.getWT() != track)
                        .collect(Collectors.toList()
                        )
        );
    }

    public static List<InfoNeed> testQueries(List<InfoNeed> residualNeeds, Track track) {
        return Collections.unmodifiableList(
                residualNeeds.stream()
                        .filter(need -> need.getWT() == track)
                        .collect(Collectors.toList()
                        )
        );
    }
}
