package edu.anadolu.knn;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import edu.anadolu.Decorator;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.cmdline.LatexTool;
import edu.anadolu.cmdline.SpamEvalTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.freq.Freq;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.cmdline.LatexTool.prettyDataSet;
import static edu.anadolu.eval.Evaluator.prettyModel;
import static edu.anadolu.knn.CartesianQueryTermSimilarity.array;
import static edu.anadolu.knn.Predict.DIV;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * k-NN without k
 */
public class XTool extends CmdLineTool {

    protected static final String BEST = "C_E_m_Ch";

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = "KStemAnchor";

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "all";

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    protected boolean catB = false;

    @Option(name = "-sigma0", required = false, usage = "use sigma0 for accuracy")
    protected boolean sigma0 = false;

    @Option(name = "-freq", required = false, usage = "Frequency implementation")
    protected Freq freq = Freq.Rel;

    @Option(name = "-numBins", required = false, usage = "number of bins")
    protected int numBins = 1000;

    @Option(name = "-var", required = false, usage = "filter training queries by variance threshold", metaVar = "0 1 2")
    protected int var = 0;

    @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    protected int spam = -1;


    @Override
    public String getShortDescription() {
        return "X Utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-test", required = false, usage = "Collection")
    protected Collection test;

    @Option(name = "-train", required = false, usage = "Collection")
    protected Collection train;

    @Option(name = "-optimize", required = false, usage = "Collection")
    protected Measure optimize = Measure.NDCG100;

    @Option(name = "-report", required = false, usage = "Collection")
    protected Measure report = Measure.NDCG100;

    protected Workbook workbook;
    protected int counter;

    private int rndIterNum = 1;

    //  Solution oracleMin = null;
    Solution oracleMax = null;
    Solution MLE = null;
    Solution RND = null;

    Solution SEL = null;

    private Solution MSSolution(int k, List<TFDAwareNeed> testNeeds, Evaluator reporter) throws IOException, InvalidFormatException {

        Path MSExcelPath = MSExcelFile();
        if (MSExcelPath == null) return null;
        Workbook workbook = WorkbookFactory.create(MSExcelFile().toFile(), null, true);

        Sheet RxT = workbook.getSheet("RxTx" + optimize.toString());

        if (RxT == null) throw new RuntimeException("RxTx" + optimize.toString() + " sheet is null!");

        Iterator<Row> iterator = RxT.rowIterator();

        Row r0 = iterator.next();

        int n = r0.getLastCellNum();


        List<TFDAwareNeed> copy = new ArrayList<>(testNeeds);

        TFDAwareNeed need = copy.remove(0);

        for (int i = 2; i < n; i++) {
            String topic = r0.getCell(i).getStringCellValue();

            if (topic.equals("T" + need.id())) {

                System.out.println("found " + topic);

                try {
                    need = copy.remove(0);
                } catch (IndexOutOfBoundsException iobe) {
                    continue;
                }
            }

        }

        if (!copy.isEmpty())
            throw new RuntimeException("copy not empty " + copy.size());

        double scores[] = new double[testNeeds.size()];
        while (iterator.hasNext()) {

            Row row = iterator.next();

            String model = row.getCell(1).getStringCellValue();


            if (("MS" + k).equals(model)) {

                copy = new ArrayList<>(testNeeds);
                TFDAwareNeed testQuery = copy.remove(0);

                int c = 0;
                for (int i = 2; i < n; i++) {

                    if (r0.getCell(i).getStringCellValue().equals("T" + testQuery.id())) {
                        scores[c++] = row.getCell(i).getNumericCellValue();

                        try {
                            testQuery = copy.remove(0);
                        } catch (IndexOutOfBoundsException ioeb) {
                            //NO-OP
                        }
                    }
                }

                if (!copy.isEmpty())
                    throw new RuntimeException("copy not empty " + copy.size());
            }

            if (("MS" + k + "L").equals(model)) {

                List<Prediction> predictionList = new ArrayList<>();

                ++counter;
                this.RxT.createRow(counter).createCell(1).setCellValue("MS" + k);

                copy = new ArrayList<>(testNeeds);


                int i = 2;
                while (!copy.isEmpty()) {

                    TFDAwareNeed testQuery = copy.remove(0);

                    while (!r0.getCell(i).getStringCellValue().equals("T" + testQuery.id())) {
                        System.out.println("excel topic header " + r0.getCell(i).getStringCellValue() + " does not match with the test query " + "T" + testQuery.id());
                        i++;
                    }

                    String predictedModel = row.getCell(i).getStringCellValue();

                    double predictedScore = reporter.score(testQuery, predictedModel);

                    if (report.equals(optimize)) {
                        if (predictedScore != scores[predictionList.size()])
                            System.out.println("=========== " + predictedModel + " " + predictedScore + " " + scores[predictionList.size()]);
                    }

                    Prediction prediction = new Prediction(testQuery, predictedModel, predictedScore);
                    predictionList.add(prediction);

                    this.RxT.getRow(counter).createCell(predictionList.size() + 1).setCellValue(predictedScore);
                }

                Solution MS = new Solution(predictionList, k);
                MS.setKey("MS" + k);
                workbook.close();
                return MS;
            }
        }

        workbook.close();
        return null;
    }


    /**
     * Excel path of Model Selection (MS7) based on train and optimize pairs.
     *
     * @return Excel file of MS7
     * @throws IOException if any
     */
    protected Path MSExcelFile() throws IOException {

        Path excelPath = Paths.get(tfd_home, train.toString()).resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        return excelPath.resolve(spam + "_MS" + train + optimize.toString() + tag + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }


    final static NormalDistribution normalDistribution = new NormalDistribution();

    protected Sheet runTopic(String sheetName, Evaluator evaluator, List<InfoNeed> residualNeeds, Map<String, Double> randomMLEMap) {

        //  oracleMin = evaluator.oracleMinAsSolution(residualNeeds);
        //final double[] oracleMinScores = oracleMin.scores;

        oracleMax = evaluator.oracleMaxAsSolution(residualNeeds);
        final double[] oracleMaxScores = oracleMax.scores;

        final double[] rndAcc = new double[residualNeeds.size()];
        Arrays.fill(rndAcc, 0.0);

        final double[] accumulator = new double[residualNeeds.size()];
        Arrays.fill(accumulator, 0.0);

        if (rndIterNum == 1) {

            RND = evaluator.randomAsSolution(residualNeeds);
            System.arraycopy(RND.scores, 0, rndAcc, 0, residualNeeds.size());

            MLE = evaluator.randomMLE(residualNeeds, randomMLEMap);
            System.arraycopy(MLE.scores, 0, accumulator, 0, residualNeeds.size());

        } else {

            double acc = 0.0;
            double sAcc = 0.0;
            for (int i = 0; i < rndIterNum; i++) {

                Solution RND = evaluator.randomAsSolution(residualNeeds);
                //  System.out.println(RND.toString());

                acc += RND.sigma1;
                sAcc += RND.sigma0;
                for (int j = 0; j < residualNeeds.size(); j++)
                    rndAcc[j] += RND.scores[j];
            }

            List<Prediction> predictionList = new ArrayList<>(residualNeeds.size());
            for (int j = 0; j < residualNeeds.size(); j++) {
                rndAcc[j] /= rndIterNum;
                predictionList.add(new Prediction(residualNeeds.get(j), null, rndAcc[j]));
            }

            RND = new Solution(predictionList, -1);
            RND.sigma1 = acc / rndIterNum;
            RND.sigma0 = sAcc / rndIterNum;
            RND.setKey("RND");
            RND.model = "RND";
            //System.out.println(RND);


            acc = 0.0;
            sAcc = 0.0;
            for (int i = 0; i < rndIterNum; i++) {

                Solution RMLE = evaluator.randomMLE(residualNeeds, randomMLEMap);
                //   System.out.println(MLE.toString());

                acc += RMLE.sigma1;
                sAcc += RMLE.sigma0;
                for (int j = 0; j < residualNeeds.size(); j++)
                    accumulator[j] += RMLE.scores[j];
            }

            predictionList.clear();
            predictionList = new ArrayList<>(residualNeeds.size());
            for (int j = 0; j < residualNeeds.size(); j++) {
                accumulator[j] /= rndIterNum;
                predictionList.add(new Prediction(residualNeeds.get(j), null, accumulator[j]));
            }

            MLE = new Solution(predictionList, -1);
            MLE.setKey("MLE");
            MLE.model = "MLE";
            MLE.sigma1 = acc / rndIterNum;
            MLE.sigma0 = sAcc / rndIterNum;
            // System.out.println(MLE);

        }


        /**
         * Insert Run X Topic Matrix as a separate sheet
         */
        Sheet RxT = workbook.createSheet(sheetName);

        List<String> models = new ArrayList<>(evaluator.getModelSet());
        Collections.sort(models);

        Row r0 = RxT.createRow(0);
        r0.createCell(1).setCellValue("Model");

        counter = 0;
        for (String model : models) {

            Row r = RxT.createRow(++counter);
            r.createCell(1).setCellValue(model);

        }

        // RxT.createRow(++counter).createCell(1).setCellValue("OracleMin");
        RxT.createRow(++counter).createCell(1).setCellValue("Oracle");
        RxT.createRow(++counter).createCell(1).setCellValue("RND");
        RxT.createRow(++counter).createCell(1).setCellValue("MLE");


        int c = 1;


        for (final InfoNeed testQuery : residualNeeds) {

            c++;
            int qID = testQuery.id();

            r0.createCell(c).setCellValue("T" + qID);

            counter = 0;
            for (String model : models) {
                Row r = RxT.getRow(++counter);
                r.createCell(c).setCellValue(evaluator.score(testQuery, model));
            }

            //    RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(oracleMinScores[c - 2]);
            RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(oracleMaxScores[c - 2]);
            RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(rndAcc[c - 2]);
            RxT.getRow(++counter).createCell(c, CellType.NUMERIC).setCellValue(accumulator[c - 2]);

        }

        return RxT;

    }

    protected String evalDirectory(DataSet dataset, Measure measure) {
        if (!dataset.spamAvailable()) {
            return "evals";
        } else if (catB && (Collection.CW09B.equals(dataset.collection()) || Collection.CW12B.equals(dataset.collection()))) {
            return "catb_evals";
        } else {

            if (spam != -1) {
                System.out.println("manuel supplied spam threshold " + spam);
                return "spam_" + spam + "_evals";
            }

            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, op);
            System.out.println("best spam threshold " + bestSpamThreshold);
            this.spam = bestSpamThreshold;
            return bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }
    }

    double[] bestSingleScores;

    protected Path excelFile() throws IOException {

        Path excelPath = Paths.get(tfd_home, test.toString()).resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        return excelPath.resolve("X" + test.toString() + report.toString() + tag + train.toString() + optimize.toString() + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }

    protected String tfd_home;

    void checkNoWinnerLoserCase() {

        for (String model : modelSet) {

            int win = (!winnerMap.containsKey(model) || winnerMap.get(model).isEmpty()) ? -1 : winnerMap.get(model).size();
            if (-1 == win)
                System.err.println(tag + " " + report + " " + optimize + " there is no winner for " + model);

            int los = (!loserMap.containsKey(model) || loserMap.get(model).isEmpty()) ? -1 : loserMap.get(model).size();

            if (-1 == los)
                System.err.println(tag + " " + report + " " + optimize + " there is no loser for " + model);

            System.out.println(model + "\twin=" + win + " los=" + los);
        }
    }

    private final Map<String, Double> similarityCache;

    public XTool() {
        workbook = new XSSFWorkbook();
        Summary = workbook.createSheet("Summary");
        similarityCache = new HashMap<>();
    }

    Sheet Summary = null;
    Sheet RxT = null;
    private int runCounter = 0;
    Set<String> modelSet = null;

    private final List<Solution> solutionList = new ArrayList<>();
    private final List<Solution> cartesianSolutionList = new ArrayList<>();

    Map<String, Set<TFDAwareNeed>> winnerMap = null;
    Map<String, Set<TFDAwareNeed>> loserMap = null;

    //  private PrintWriter out;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        final DataSet testDataSet = CollectionFactory.dataset(test, tfd_home);
        final DataSet trainDataSet = CollectionFactory.dataset(train, tfd_home);

        final Decorator testDecorator = new Decorator(testDataSet, tag, freq, numBins);
        final Decorator trainDecorator = new Decorator(trainDataSet, tag, freq, numBins);

        final String eval_dir = evalDirectory(trainDataSet, optimize);

        final Evaluator trainEvaluator = new Evaluator(trainDataSet, tag, optimize, models, evalDirectory(trainDataSet, optimize), op);
        final Evaluator testEvaluator = new Evaluator(testDataSet, tag, report, trainEvaluator.models(), eval_dir, op);


        modelSet = trainEvaluator.getModelSet();


        List<InfoNeed> residualNeeds = testEvaluator.residualNeeds();
        residualNeedsSize = residualNeeds.size();
        System.out.println("residual Size " + residualNeedsSize + " all " + testEvaluator.getNeeds().size() + " same =" + testEvaluator.getAllSame().size() + " zero = " + testEvaluator.getAllZero().size());
        List<TFDAwareNeed> testQueries = testDecorator.residualTFDAwareNeeds(residualNeeds);

        RxT = runTopic("RxTx" + report.toString(), testEvaluator, residualNeeds, trainEvaluator.randomMLEMap());


        List<Solution> accuracyList = testEvaluator.modelsAsSolutionList(residualNeeds);

        Solution bestSingle = Collections.max(accuracyList, (o1, o2) -> (int) Math.signum(o1.sigma1 - o2.sigma1));
        bestSingleScores = bestSingle.scores;

        // Solution bestSingleHit = Collections.max(accuracyList, (o1, o2) -> (int) Math.signum(o1.sigma1 - o2.sigma1));
        // bestSingleHit.model = "MLE";


        accuracyList.sort((Solution o1, Solution o2) -> (int) Math.signum(o2.sigma1 - o1.sigma1));
        System.out.println("Number of hits (percentage) : ");
        accuracyList.forEach(System.out::println);


        /**
         * T-statistics separate sheet, Run X Percentages for Best Single Run
         */
        Summary.createRow(0).createCell(1, CellType.STRING).setCellValue("T-stats");
        Summary.getRow(0).createCell(2, CellType.STRING).setCellValue("W-stats");
        Summary.getRow(0).createCell(3, CellType.STRING).setCellValue(report.toString());
        Summary.getRow(0).createCell(4, CellType.STRING).setCellValue("sigma1");
        Summary.getRow(0).createCell(5, CellType.STRING).setCellValue("sigma0");


        List<Solution> trainList = trainEvaluator.modelsAsSolutionList(trainEvaluator.residualNeeds());
        Solution bestSingleHitFromTrain = Collections.max(trainList, (o1, o2) -> (int) Math.signum(o1.sigma1 - o2.sigma1));

        final String singleHitModelFromTraining = bestSingleHitFromTrain.list.get(0).predictedModel;

        for (Prediction prediction : bestSingleHitFromTrain.list) {
            if (!singleHitModelFromTraining.equals(prediction.predictedModel))
                throw new RuntimeException("list of predicted models should be same!");
        }


        Solution SGL = testEvaluator.modelAsSolution(testEvaluator.prettify(singleHitModelFromTraining), residualNeeds);

        SGL.model = "SGL";
        SGL.key = "SGL";
        SGL.predict = Predict.DIV;


        if (var == 0) {
            winnerMap = trainDecorator.decorate(trainEvaluator.bestModelMap);
            loserMap = trainDecorator.decorate(trainEvaluator.worstModelMap);
        } else {
            winnerMap = trainDecorator.decorate(trainEvaluator.filter(trainEvaluator.bestModelMap, var));
            loserMap = trainDecorator.decorate(trainEvaluator.filter(trainEvaluator.worstModelMap, var));
        }

        checkNoWinnerLoserCase();

        //out = new PrintWriter(Files.newBufferedWriter(testDataSet.collectionPath().resolve(test + tag + optimize + "WinLoseSetSimilarity" + (var == 0 ? "" : "Var" + var) + "Features.txt"), StandardCharsets.US_ASCII));
        //out.println("#This is a tab-separated file, which generated by ./run.sh X -test CW09B -train CW09B -optimize NDCG100 -report NDCG100 -tag " + tag);
        // out.print("QueryID");
        // for (String model : modelSet) {
        //     out.print("," + prettyModel(model) + "Win," + prettyModel(model) + "Lose");
        // }
        // out.println();
        doSelectiveTermWeighting(testQueries, testEvaluator);
        // out.flush();
        // out.close();

        Solution MS = MSSolution(7, testQueries, testEvaluator);
        if (MS != null)
            testEvaluator.calculateAccuracy(MS);

        Map<String, Double> geoRiskMap = addGeoRisk2Sheet(RxT);

        accuracyList.sort(Comparator.comparing(o -> o.key));
        accuracyList.forEach(this::writeSolution2SummarySheet);


        writeSolution2SummarySheet(SGL);
        writeSolution2SummarySheet(oracleMax);
        writeSolution2SummarySheet(RND);
        writeSolution2SummarySheet(MLE);


        accuracyList.add(oracleMax);
        accuracyList.add(RND);
        accuracyList.add(MLE);
        accuracyList.add(SEL);

        if (MS != null)
            accuracyList.add(MS);

        accuracyList.forEach(solution -> {
            solution.geoRisk = geoRiskMap.get(solution.key);
        });


        latex(accuracyList);
        dumpRankInfo();


        MLE.predict = Predict.DIV;
        // solutionList.add(MLE);
        // solutionList.add(SGL);
        persistSolutionsLists();

    }

    private static List<Integer> ranks(int size) {
        List<Integer> ranks = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            ranks.add(i);

        return ranks;
    }

    private static List<Integer> ranksSigma0(List<Solution> solutionList) {

        List<Integer> ranks = ranks(solutionList.size());

        ranks.sort((o1, o2) -> Double.compare(solutionList.get(o2).sigma0, solutionList.get(o1).sigma0));

        return ranks;
    }

    private static List<Integer> ranksSigma1(List<Solution> solutionList) {

        List<Integer> ranks = ranks(solutionList.size());

        ranks.sort((o1, o2) -> Double.compare(solutionList.get(o2).sigma1, solutionList.get(o1).sigma1));

        return ranks;
    }

    private static List<Integer> ranksMean(final List<Solution> solutionList) {

        List<Integer> ranks = ranks(solutionList.size());

        ranks.sort((o1, o2) -> Double.compare(solutionList.get(o2).mean, solutionList.get(o1).mean));

        return ranks;
    }

    private static List<Integer> ranksRisk(final List<Solution> solutionList) {

        List<Integer> ranks = ranks(solutionList.size());

        ranks.sort((o1, o2) -> Double.compare(solutionList.get(o2).geoRisk, solutionList.get(o1).geoRisk));

        return ranks;
    }

    private static Solution sel(List<Solution> solutionList) {

        for (Solution solution : solutionList)
            if (("DIV_" + BEST).equals(solution.key))
                return solution;

        throw new RuntimeException("cannot find selective model in the list!");
    }

    private boolean isSameT(Solution sel, Solution other) {
        return !tTest.pairedTTest(sel.scores, other.scores, 0.05);
    }


    private boolean isSameW(Solution sel, Solution other) {

        double p = wilcoxon.wilcoxonSignedRankTest(sel.scores, other.scores, false);

        return p >= 0.05;
    }

    class RankInfo {
        int accuracy;
        int effectiveness;
        int robustness;
        List<String> InSignificant = new ArrayList<>();

        String ranks() {
            return String.format("%d %d %d", accuracy, effectiveness, robustness);
        }

        String models() {

            if (InSignificant.isEmpty()) return "";

            StringBuilder builder = new StringBuilder();

            InSignificant.forEach(s -> builder.append(s).append(","));

            builder.deleteCharAt(builder.length() - 1);

            return builder.toString();
        }
    }

    void dumpRankInfo() {

        Sheet sheet = workbook.createSheet("RankInfo");
        Row row = sheet.createRow(0);

        row.createCell(0).setCellValue(rankInfo.ranks());
        row.createCell(1).setCellValue(rankInfo.models());
    }

    RankInfo rankInfo = null;

    protected String header() {
        String header = "\\begin{table}\n" +
                "\\caption{\n" +
                "Selective term-weighting result for %s dataset (\\texttt{%s}) over %d queries. \n" +
                "Retrieval effectiveness is measured by %s.\n" +
                "The models that are \\emph{not} statistically different ($p<0.05$) from the selective approach (\\texttt{SEL}) are marked with: \n" +
                "$\\dagger$ symbol according to the paired $t$-test and \n" +
                "$\\aleph$ symbol according to the Wilcoxon signed-rank test. \n" +
                "}\n" +
                "\\label{tbl:%s}\n" +
                "\\centering\n" +
                "\\begin{tabular}{| c | c | c | c | c | c | c | c |}\n" +
                "\\hline \n" +
                "&   \\multicolumn{3}{c|}{Accuracy \\pct} & \\multicolumn{2}{c|}{Effectiveness} &  \\multicolumn{2}{c|}{Robustness}\\\\\n" +
                "\\hline \n" +
                "Model & $\\sigma=0$ & $\\sigma=1$ & Rank & %s & Rank & GeoRisk & Rank \\\\\n" +
                "\\hline\n";

        if (sigma0)
            header = "\\begin{table}\n" +
                    "\\caption{\n" +
                    "Selective term-weighting result for %s dataset (\\texttt{%s}) over %d queries. \n" +
                    "Retrieval effectiveness is measured by %s.\n" +
                    "The models that are \\emph{not} statistically different ($p<0.05$) from the selective approach (\\texttt{SEL}) are marked with: \n" +
                    "$\\dagger$ symbol according to the paired $t$-test and \n" +
                    "$\\aleph$ symbol according to the Wilcoxon signed-rank test. \n" +
                    "}\n" +
                    "\\label{tbl:%s}\n" +
                    "\\centering\n" +
                    "\\begin{tabular}{| c | c | c | c | c | c | c |}\n" +
                    "\\hline \n" +
                    "&   \\multicolumn{2}{c|}{Accuracy} & \\multicolumn{2}{c|}{Effectiveness} &  \\multicolumn{2}{c|}{Robustness}\\\\\n" +
                    "\\hline \n" +
                    "Model & \\pct & Rank & %s & Rank & GeoRisk & Rank \\\\\n" +
                    "\\hline\n";

        return header;
    }

    protected String latexHeader() {

        String anchor = "KStemAnchor".equals(tag) ? "Anchor" : "NoAnchor";

        String tableName = test.toString() + report.toString() + train.toString() + optimize.toString() + anchor + "spam" + spam;
        String header = header();
        return String.format(header, prettyDataSet(test.toString()), anchor, residualNeedsSize, report.toString(), tableName, report.toString());
    }


    String latex(List<Solution> solutionList) {

        StringBuilder builder = new StringBuilder(latexHeader());

        rankInfo = new RankInfo();

        solutionList.sort((Solution o1, Solution o2) -> Double.compare(o2.mean, o1.mean));

        List<Integer> ranksRisk = ranksRisk(solutionList);
        List<Integer> ranksSigma1 = sigma0 ? ranksSigma0(solutionList) : ranksSigma1(solutionList);

        Solution sel = sel(solutionList);

        for (int i = 0; i < solutionList.size(); i++) {

            Solution s = solutionList.get(i);

            int rankSigma1 = ranksSigma1.indexOf(i);
            int rankRisk = ranksRisk.indexOf(i);
            String line;
            if (sigma0)
                line = String.format("%s & %.2f & %d & %.4f & %d & %.4f & %d \\\\", LatexTool.latexModel(s.key, false), s.sigma0, rankSigma1, s.mean, i, s.geoRisk, rankRisk);
            else
                line = String.format("%s & %.2f & %.2f & %d & %.4f & %d & %.4f & %d \\\\", LatexTool.latexModel(s.key), s.sigma0, s.sigma1, rankSigma1, s.mean, i, s.geoRisk, rankRisk);

            if (isSameT(sel, s) || sel.key.equals(s.key))
                line = "$^\\dagger$" + line;

            if (isSameW(sel, s) || sel.key.equals(s.key))
                line = "$^\\aleph$" + line;

            if (sel.key.equals(s.key)) {
                rankInfo.accuracy = rankSigma1;
                rankInfo.effectiveness = i;
                rankInfo.robustness = rankRisk;
            }

            if (!sel.key.equals(s.key)) {

                String marker = "";

                if (isSameT(sel, s))
                    marker = "$^\\dagger$" + marker;

                if (isSameW(sel, s))
                    marker = "$^\\aleph$" + marker;


                if (isSameT(sel, s) || isSameW(sel, s))
                    rankInfo.InSignificant.add(marker + Evaluator.prettyModel(s.key));
            }

            builder.append(line).append("\n");

            System.err.println(String.format(" \\multicolumn{1}{l|}{%s} &  %.4f & %.4f &  %d  &  %d &  %d & %d \\\\", LatexTool.latexModel(s.key, false), s.mean, s.geoRisk, rankRisk, s.hits0, s.hits1, s.hits2));
        }

        builder.append("\\hline\n" +
                "\\end{tabular}\n" +
                "\\end{table}\n\n");

        System.out.println(builder.toString());
        return builder.toString();
    }

    void persistSolutionsLists() throws IOException {
        System.out.println("Result sorted by mean effectiveness");
        solutionList.sort((Solution o1, Solution o2) -> Double.compare(o2.mean, o1.mean));
        solutionList.forEach(System.out::println);


        System.out.println("Result sorted by sigma0");
        solutionList.sort((Solution o1, Solution o2) -> Double.compare(o2.sigma0, o1.sigma0));
        solutionList.forEach(System.out::println);


        System.out.println("====== Interaction Plot Data");

        printInterActionPlot("IPxA", solutionList, new String[]{"MODEL", "PRED"});

        System.out.println("====== Interaction Plot Data for Cartesian Only");

        printInterActionPlot("IPxAxCar", cartesianSolutionList, new String[]{"MODEL", "AGG", "PRED"});

        System.out.println("Cartesian Results sorted by sigma0");
        cartesianSolutionList.sort((Solution o1, Solution o2) -> Double.compare(o2.sigma0, o1.sigma0));
        cartesianSolutionList.forEach(System.out::println);


        Path excelFile = excelFile();
        Files.deleteIfExists(excelFile);
        workbook.write(Files.newOutputStream(excelFile(), CREATE_NEW, WRITE));
        workbook.close();
    }

    public Map<String, Double> addGeoRisk2Sheet(Sheet sheet) {

        Map<String, Double> geoRiskMap = new HashMap<>();

        int c = sheet.getRow(0).getLastCellNum();
        int r = sheet.getLastRowNum() + 1;


        sheet.getRow(0).createCell(0, CellType.STRING).setCellValue("geoRisk");

        final double[] rowSum = new double[r];
        Arrays.fill(rowSum, 0.0);

        final double[] columnSum = new double[c];
        Arrays.fill(columnSum, 0.0);

        double N = 0.0;

        for (int i = 1; i < r; i++) {

            Row row = sheet.getRow(i);

            for (int j = 2; j < c; j++) {

                if (row.getCell(j) == null) {
                    // throw new RuntimeException("encountered null cell i=" + i + " j=" + j + " during geoZrisk addition");
                    System.out.println("encountered null cell i=" + i + " j=" + j + " during geoZrisk addition");
                    continue;
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

        for (int i = 1; i < r; i++) {

            Row row = sheet.getRow(i);
            double zRisk = 0.0;


            int counter = 0;
            for (int j = 2; j < c; j++) {

                final double cellValue = row.getCell(j).getNumericCellValue();
                final double e = rowSum[i] * columnSum[j] / N;

                counter++;

                zRisk += (cellValue - e) / Math.sqrt(e);

            }


            final double geoRisk = Math.sqrt(rowSum[i] / counter * normalDistribution.cumulativeProbability(zRisk / counter));

            row.createCell(0, CellType.NUMERIC).setCellValue(geoRisk);
            geoRiskMap.put(row.getCell(1).getStringCellValue(), geoRisk);

        }

        return geoRiskMap;

    }

    private double sumOf(List<ModelScore> list) {
        return list.stream().mapToDouble(ModelScore::score).sum();
    }

    void doSelectiveTermWeighting(List<TFDAwareNeed> testQueries, Evaluator testEvaluator) {
        for (final QuerySimilarity querySimilarity : KNNTool.querySimilarities()) {

            for (Predict predict : new Predict[]{DIV}) {

                Map<String, Integer> affairs = new HashMap<>();

                final String key = predict.toString() + "_" + querySimilarity.toString();

                if (DIV.equals(predict) && querySimilarity instanceof CartesianQueryTermSimilarity && BEST.equals(querySimilarity.name())) {
                    ++counter;
                    RxT.createRow(counter).createCell(1).setCellValue(key);
                }


                List<Prediction> predictionList = new ArrayList<>();


                int column = 1;


                for (final TFDAwareNeed testQuery : testQueries) {

                    column++;
                    String predictedModel = null;
                    switch (predict) {
                        case WIN: {
                            List<ModelScore> list = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            ModelScore closest = Collections.min(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = closest.model;
                            break;
                        }

                        case LOS: {

                            List<ModelScore> list = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);
                            ModelScore farthest = Collections.max(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = farthest.model;
                            break;

                        }

                        case HYB: {

                            List<ModelScore> win = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            List<ModelScore> los = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);

                            List<ModelScore> list = new ArrayList<>();

                            int i = 0;
                            for (String model : modelSet) {

                                ModelScore winS = win.get(i);
                                if (!winS.model.equals(model))
                                    throw new RuntimeException("winS " + winS.model + " is not equal to " + model + "!");

                                ModelScore losS = los.get(i);
                                if (!losS.model.equals(model))
                                    throw new RuntimeException("losS " + losS.model + " is not equal to " + model + "!");

                                list.add(new ModelScore(model, (winS.score + 1.0) / (losS.score + 1.0)));
                                i++;
                            }

                            ModelScore closest = Collections.min(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = closest.model;
                            break;

                        }
                        case DIV: {

                            List<ModelScore> win = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            List<ModelScore> los = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);

                            List<ModelScore> list = new ArrayList<>();

                            int i = 0;
                            for (String model : modelSet) {

                                ModelScore winS = win.get(i);
                                if (!winS.model.equals(model))
                                    throw new RuntimeException("winS " + winS.model + " is not equal to " + model + "!");

                                ModelScore losS = los.get(i);
                                if (!losS.model.equals(model))
                                    throw new RuntimeException("losS " + losS.model + " is not equal to " + model + "!");

                                list.add(new ModelScore(model, (winS.score) / (losS.score)));

                                i++;
                            }

                            ModelScore closest = Collections.min(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = closest.model;
                            break;

                        }

                        case ODD: {

                            List<ModelScore> win = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            List<ModelScore> los = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);

                            double sumWin = sumOf(win);
                            double sumLos = sumOf(los);

                            List<ModelScore> list = new ArrayList<>();

                            int i = 0;
                            for (String model : modelSet) {

                                ModelScore winS = win.get(i);
                                if (!winS.model.equals(model))
                                    throw new RuntimeException("winS " + winS.model + " is not equal to " + model + "!");

                                ModelScore losS = los.get(i);
                                if (!losS.model.equals(model))
                                    throw new RuntimeException("losS " + losS.model + " is not equal to " + model + "!");

                                list.add(new ModelScore(model, (winS.score / sumWin) / (losS.score / sumLos)));
                                i++;
                            }

                            ModelScore closest = Collections.min(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = closest.model;
                            break;

                        }

                        case DIS: {

                            List<ModelScore> win = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            List<ModelScore> los = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);

                            List<ModelScore> list = new ArrayList<>();

                            int i = 0;
                            for (String model : modelSet) {

                                ModelScore winS = win.get(i);
                                if (!winS.model.equals(model))
                                    throw new RuntimeException("winS " + winS.model + " is not equal to " + model + "!");

                                ModelScore losS = los.get(i);
                                if (!losS.model.equals(model))
                                    throw new RuntimeException("losS " + losS.model + " is not equal to " + model + "!");

                                list.add(new ModelScore(model, winS.score - losS.score));
                                i++;
                            }

                            ModelScore closest = Collections.min(list, (ModelScore o1, ModelScore o2) -> (int) Math.signum(o1.score - o2.score));
                            predictedModel = closest.model;
                            break;

                        }
                        case RNK: {

                            List<ModelScore> win = distance2Models(testQuery, winnerMap, querySimilarity, Double.MAX_VALUE);
                            List<ModelScore> los = distance2Models(testQuery, loserMap, querySimilarity, Double.MAX_VALUE);

                            win.sort((o1, o2) -> (int) Math.signum(o2.score - o1.score));
                            los.sort((o1, o2) -> (int) Math.signum(o1.score - o2.score));

                            List<Rank> ranks = new ArrayList<>();
                            for (String model : modelSet)
                                ranks.add(new Rank(model));

                            for (int i = 0; i < win.size(); i++) {

                                for (Rank rank : ranks) {
                                    if (rank.model.equals(win.get(i).model))
                                        rank.winRank = i + 1;

                                    if (rank.model.equals(los.get(i).model))
                                        rank.losRank = i + 1;
                                }

                            }

                            Rank rank = Collections.max(ranks, Comparator.comparing(Rank::e));
                            predictedModel = rank.model;
                            break;

                        }

                        default:
                            throw new AssertionError(this);
                    }


                    if (null == predictedModel) throw new RuntimeException("predictedModel is null!");

                    final double predictedScore = testEvaluator.score(testQuery, predictedModel);

                    final Prediction predicted = new Prediction(testQuery, predictedModel, predictedScore);

                    if (DIV.equals(predict) && querySimilarity instanceof CartesianQueryTermSimilarity && BEST.equals(querySimilarity.name())) {
                        RxT.getRow(counter).createCell(column).setCellValue(predicted.predictedScore);
                        System.out.println(testQuery.id() + " " + predictedModel);
                    }


                    predictionList.add(predicted);

                    if (testEvaluator.multiLabelMap(1.0).get(testQuery).contains(predictedModel)) {
                        incrementAffairs(affairs, predictedModel + "_" + predictedModel);
                    } else {
                        for (String actualModel : testEvaluator.multiLabelMap(1.0).get(testQuery)) {
                            incrementAffairs(affairs, predictedModel + "_" + actualModel);
                        }
                    }

                }

                writeAffairs(key, affairs);
                affairs.clear();

                /**
                 * t statistics excel sheet
                 */

                Solution solution = new Solution(predictionList, -1);
                solution.setKey(key);
                testEvaluator.calculateAccuracy(solution);


                solution.model = querySimilarity.name();
                solution.predict = predict;

                if (DIV.equals(predict) && querySimilarity instanceof CartesianQueryTermSimilarity && BEST.equals(querySimilarity.name())) {
                    SEL = solution.clone();
                }

                writeSolution2SummarySheet(solution);

                if (querySimilarity instanceof CartesianQueryTermSimilarity) {
                    if (BEST.equals(solution.model))
                        solutionList.add(solution);
                } else
                    solutionList.add(solution);

                if (querySimilarity instanceof CartesianQueryTermSimilarity) {

                    Solution clone = solution.clone();

                    clone.model = querySimilarity.name().charAt(0) + "";

                    clone.agg = ((CartesianQueryTermSimilarity) querySimilarity).aggregation();
                    cartesianSolutionList.add(clone);

                }

                if (predictionList.size() != residualNeedsSize)
                    throw new RuntimeException("list sizes are not equal!");


            }
        }
    }

    int residualNeedsSize;

    private double similarity(QuerySimilarity querySimilarity, TFDAwareNeed testQuery, TFDAwareNeed trainQuery) {

        final String key1 = querySimilarity.toString() + "_" + testQuery.id() + "_" + trainQuery.id();
        final String key2 = querySimilarity.toString() + "_" + trainQuery.id() + "_" + testQuery.id();

        final double sim;
        if (similarityCache.containsKey(key1)) {
            sim = similarityCache.get(key1);
        } else if (similarityCache.containsKey(key2)) {
            sim = similarityCache.get(key2);
        } else {
            sim = querySimilarity.score(testQuery, trainQuery);
            if (Double.isInfinite(sim) || Double.isNaN(sim) || sim < 0) {
                throw new RuntimeException(querySimilarity.toString() + " sim " + sim + " test " + testQuery + " train" + trainQuery);
            }
            similarityCache.put(key1, sim);

            double sym = querySimilarity.score(trainQuery, testQuery);
            if (sim != sym)
                throw new RuntimeException(querySimilarity.toString() + " is not symmetric! " + sim + " " + sym + " " + trainQuery.getDistinctQuery() + " " + testQuery.getDistinctQuery());

            else
                similarityCache.put(key2, sym);
        }

        return sim;
    }


    private List<ModelScore> distance2Models(TFDAwareNeed testQuery, Map<String, Set<TFDAwareNeed>> winnerMap, QuerySimilarity querySimilarity, double none) {

        List<ModelScore> list = new ArrayList<>();

        for (String model : modelSet) {

            Set<TFDAwareNeed> trainingQueries = winnerMap.get(model);

            if (null == trainingQueries || trainingQueries.size() == 0) {
                list.add(new ModelScore(model, none));
                continue;
            }

            double distance = 0.0;
            int d = 0;
            for (TFDAwareNeed trainQuery : trainingQueries) {

                if (trainQuery.id() == testQuery.id()) continue;

                distance += similarity(querySimilarity, testQuery, trainQuery);
                d++;
            }


            distance /= d;

            list.add(new ModelScore(model, distance));

        }

        return list;
    }


    final TTest tTest = new TTest();
    final WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();

    double t(final double[] x, final double[] y) {
        return tTest.pairedTTest(x, y);
    }

    private double z(final double[] x, final double[] y) {
        return wilcoxon.wilcoxonSignedRankTest(x, y, false);
    }

    protected void writeSolution2SummarySheet(Solution solution) {
        runCounter++;
        Summary.createRow(runCounter).createCell(0, CellType.STRING).setCellValue(solution.key);
        Summary.getRow(runCounter).createCell(1, CellType.NUMERIC).setCellValue(t(bestSingleScores, solution.scores));
        Summary.getRow(runCounter).createCell(2, CellType.NUMERIC).setCellValue(z(bestSingleScores, solution.scores));
        Summary.getRow(runCounter).createCell(3, CellType.NUMERIC).setCellValue(solution.getMean());
        Summary.getRow(runCounter).createCell(4, CellType.NUMERIC).setCellValue(solution.sigma1);
        Summary.getRow(runCounter).createCell(5, CellType.NUMERIC).setCellValue(solution.sigma0);
    }

    protected void writeModel2ModelMatrixHeaders(Sheet sheet, Set<String> modelSet, Map<String, Integer> affairs) {
        Row r0 = sheet.createRow(0);

        int c = 1;
        for (String model : modelSet) {
            r0.createCell(c++, CellType.STRING).setCellValue(model);
        }

        c = 1;
        for (String model : modelSet) {
            sheet.createRow(c++).createCell(0, CellType.STRING).setCellValue(model);
        }


        int i = 1;
        for (String predicted : modelSet) {
            int j = 1;
            for (String actual : modelSet) {
                String k = predicted + "_" + actual;

                final int count = affairs.getOrDefault(k, 0);

                sheet.getRow(i).createCell(j, CellType.NUMERIC).setCellValue(count);


                j++;
            }
            i++;
        }
    }

    private void writeAffairs(String key, Map<String, Integer> affairs) {

        Sheet sheet;
        try {
            sheet = workbook.createSheet(key);
        } catch (java.lang.IllegalArgumentException e) {
            sheet = workbook.createSheet(key + "1");
        }

        writeModel2ModelMatrixHeaders(sheet, modelSet, affairs);

        int c = modelSet.size() + 1;

        Row r;

        int i;

        r = sheet.createRow(++c);
        r.createCell(0, CellType.STRING).setCellValue("hitCount");

        i = 1;
        for (String model : modelSet) {
            r.createCell(i++, CellType.NUMERIC).setCellValue(winnerMap.get(model) == null ? -1 : winnerMap.get(model).size());
        }

        r = sheet.createRow(++c);
        r.createCell(0, CellType.STRING).setCellValue("failCount");

        i = 1;
        for (String model : modelSet) {
            r.createCell(i++, CellType.NUMERIC).setCellValue(loserMap.get(model) == null ? -1 : loserMap.get(model).size());
        }


    }

    static protected void incrementAffairs(Map<String, Integer> affairs, String k) {
        if (affairs.containsKey(k)) {
            int c = affairs.get(k);
            c++;
            affairs.put(k, c);
        } else
            affairs.put(k, 1);
    }

    private static void addItem2Map(Map<String, Set<TFDAwareNeed>> winnerMap, TFDAwareNeed need, String model) {
        if (winnerMap.containsKey(model)) {
            Set<TFDAwareNeed> set = winnerMap.get(model);
            if (set.contains(need))
                throw new RuntimeException("set already contains " + need.toString());
            set.add(need);
            winnerMap.put(model, set);
        } else {
            Set<TFDAwareNeed> set = new HashSet<>();
            set.add(need);
            winnerMap.put(model, set);
        }
    }

    private static Map<String, Integer> countMap(Map<String, List<InfoNeed>> map) {
        Map<String, Integer> countMap = new HashMap<>();

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {

            if (entry.getValue().isEmpty())
                throw new RuntimeException("winner set is empty for " + entry.getKey().toString());

            countMap.put(entry.getKey(), entry.getValue().size());
        }

        return countMap;
    }

//    private static Map<String, Integer> countMap(Map<InfoNeed, Set<String>> labelMap) {
//        Map<String, Integer> countMap = new HashMap<>();
//
//        for (Map.Entry<InfoNeed, Set<String>> entry : labelMap.entrySet()) {
//
//            if (entry.getValue().isEmpty())
//                throw new RuntimeException("winner set is empty for " + entry.getKey().toString());
//
//            for (String model : entry.getValue())
//                incrementAffairs(countMap, model);
//        }
//
//        return countMap;
//    }

    private Map<String, Set<TFDAwareNeed>> tieBreaker(Decorator decorator, QuerySimilarity similarity, Map<InfoNeed, Set<String>> labelMap) {


        Map<String, Set<TFDAwareNeed>> winnerMap = new HashMap<>();

        // add non-tie winners

        for (Map.Entry<InfoNeed, Set<String>> entry : labelMap.entrySet()) {

            if (entry.getValue().isEmpty())
                throw new RuntimeException("winner set is empty for " + entry.getKey().toString());

            if (entry.getValue().size() != 1) continue;

            String model = entry.getValue().iterator().next();

            TFDAwareNeed need = decorator.tfdAwareNeed(entry.getKey());
            addItem2Map(winnerMap, need, model);

        }


        // add tie winners

        Map<TFDAwareNeed, String> tie = new HashMap<>();

        for (Map.Entry<InfoNeed, Set<String>> entry : labelMap.entrySet()) {

            if (entry.getValue().size() == 1) continue;

            TFDAwareNeed testQuery = decorator.tfdAwareNeed(entry.getKey());

            String predictedModel = null;
            double minDistance = Double.POSITIVE_INFINITY;

            for (String model : entry.getValue()) {

                if (!winnerMap.containsKey(model)) continue;

                double distance = 0.0;
                int d = 0;
                for (TFDAwareNeed trainingQuery : winnerMap.get(model)) {

                    if (trainingQuery.id() == testQuery.id())
                        throw new RuntimeException("test query shouldn't exists in the winner list!");

                    final double sim = similarity.score(testQuery, trainingQuery);

                    if (Double.isInfinite(sim) || Double.isNaN(sim)) {
                        throw new RuntimeException(test.toString() + train.toString() + " " + tag + " " + report + " " + optimize + " " + similarity.toString() + " sim " + sim + " test " + testQuery + " train" + trainingQuery);
                    }

                    distance += sim;
                    d++;

                }

                distance /= d;

                if (distance < minDistance) {
                    predictedModel = model;
                    minDistance = distance;
                }
            }

            if (null == predictedModel) throw new RuntimeException("predictedModel is null!");

            tie.put(testQuery, predictedModel);

        }
        for (Map.Entry<TFDAwareNeed, String> entry : tie.entrySet()) {
            addItem2Map(winnerMap, entry.getKey(), entry.getValue());
        }

        return winnerMap;
    }

    private Map<String, Set<TFDAwareNeed>> prettyKeys(Map<String, Set<TFDAwareNeed>> winnerMap) {
        Map<String, Set<TFDAwareNeed>> prettyWinnerMap = new HashMap<>(winnerMap.size());

        for (String oldkey : winnerMap.keySet()) {
            String newkey = prettyModel(oldkey);
            prettyWinnerMap.put(newkey, winnerMap.get(oldkey));
        }

        winnerMap.clear();
        return prettyWinnerMap;
    }

    private void printInterActionPlot(String sheetName, List<Solution> solutionList) {

        Sheet sheet = workbook.createSheet(sheetName);

        Row r0 = sheet.createRow(0);


        r0.createCell(0, CellType.STRING).setCellValue("MODEL");
        r0.createCell(1, CellType.STRING).setCellValue("TEST");
        r0.createCell(2, CellType.STRING).setCellValue("TRAIN");
        r0.createCell(3, CellType.STRING).setCellValue("REPORT");
        r0.createCell(4, CellType.STRING).setCellValue("OPTIMIZE");
        r0.createCell(5, CellType.STRING).setCellValue("ACC");

        int r = 1;
        for (Solution solution : solutionList) {

            Row row = sheet.createRow(r++);

            final String model = "C".equals(solution.model) ? solution.agg : solution.model;

            row.createCell(0, CellType.STRING).setCellValue(model);
            row.createCell(1, CellType.STRING).setCellValue(test.toString());
            row.createCell(2, CellType.STRING).setCellValue(train.toString());
            row.createCell(3, CellType.STRING).setCellValue(report.toString());
            row.createCell(4, CellType.STRING).setCellValue(optimize.toString());
            row.createCell(5, CellType.NUMERIC).setCellValue(solution.sigma1);

        }
    }


    private void printInterActionPlot(String sheetName, List<Solution> solutionList, String[] attributes) {

        Sheet sheet = workbook.createSheet(sheetName);

        Row r0 = sheet.createRow(0);

        for (int i = 0; i < attributes.length; i++)
            r0.createCell(i, CellType.STRING).setCellValue(attributes[i]);


        r0.createCell(attributes.length, CellType.STRING).setCellValue("TEST");
        r0.createCell(attributes.length + 1, CellType.STRING).setCellValue("TRAIN");
        r0.createCell(attributes.length + 2, CellType.STRING).setCellValue("REPORT");
        r0.createCell(attributes.length + 3, CellType.STRING).setCellValue("OPTIMIZE");
        r0.createCell(attributes.length + 4, CellType.STRING).setCellValue("ACC");

        ListMultimap<String, Double> interactionMap = ArrayListMultimap.create();
        for (Solution solution : solutionList) {
            StringBuilder key = new StringBuilder();
            for (String attr : attributes) {
                key.append(solution.attr(attr)).append(",");
            }
            interactionMap.put(deleteLastChar(key).toString(), solution.sigma1);
        }

        Map<String, StringBuilder> builderMap = new LinkedHashMap<>();

        for (String att : attributes) {
            StringBuilder builder = new StringBuilder(att);
            builder.append(" = {");
            builderMap.put(att, builder);
        }

        StringBuilder x_means = new StringBuilder("ACC = [");

        int r = 1;
        for (Map.Entry<String, java.util.Collection<Double>> entry : interactionMap.asMap().entrySet()) {

            String key = entry.getKey();
            java.util.Collection<Double> list = entry.getValue();

            System.out.println(list.size() + "\t" + key);

            double mean = StatUtils.mean(array(list));

            String[] parts = key.split(",");

            if (parts.length != attributes.length) throw new RuntimeException("parts.length != attributes.length");

            for (int i = 0; i < attributes.length; i++)
                builderMap.get(attributes[i]).append("'").append(parts[i]).append("',");

            x_means.append(mean).append(" ");

            Row row = sheet.createRow(r++);

            for (int i = 0; i < attributes.length; i++)
                row.createCell(i, CellType.STRING).setCellValue(parts[i]);

            row.createCell(attributes.length, CellType.STRING).setCellValue(null == test ? "NULL" : test.toString());
            row.createCell(attributes.length + 1, CellType.STRING).setCellValue(null == train ? "NULL" : train.toString());
            row.createCell(attributes.length + 2, CellType.STRING).setCellValue(null == report ? "NULL" : report.toString());
            row.createCell(attributes.length + 3, CellType.STRING).setCellValue(null == optimize ? "NULL" : optimize.toString());
            row.createCell(attributes.length + 4, CellType.NUMERIC).setCellValue(mean);

        }

        for (StringBuilder builder : builderMap.values()) {
            deleteLastChar(builder).append("}'");
        }

        deleteLastChar(x_means).append("]'");

        builderMap.values().forEach(System.out::println);

        System.out.println(x_means);
        StringBuilder group = new StringBuilder("{");
        StringBuilder varNames = new StringBuilder("{");

        for (String attr : attributes) {
            group.append(attr).append(",");
            varNames.append("'").append(attr).append("',");
        }

        deleteLastChar(group).append("}");
        deleteLastChar(varNames).append("}");

        System.out.println("interactionplot(ACC, " + group.toString() + ", 'varnames', " + varNames.toString() + ")");
    }

    private StringBuilder deleteLastChar(StringBuilder builder) {
        builder.deleteCharAt(builder.length() - 1);
        return builder;
    }

}
