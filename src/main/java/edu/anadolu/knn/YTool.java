package edu.anadolu.knn;

import edu.anadolu.Decorator;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.RankingAlgorithm;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;
import org.paukov.combinatorics3.Generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static edu.anadolu.cmdline.LatexTool.prettyDataSet;

/**
 * Appends collections and then performs selective term-weighting
 */
public class YTool extends XTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    private void setCollection(String s) {
        String[] parts = s.split("_");
        collections = new Collection[parts.length];
        for (int i = 0; i < parts.length; i++) {
            collections[i] = Collection.valueOf(parts[i]);
        }
        Arrays.sort(collections);
    }

    private Collection[] collections;

    @Option(name = "-msK", required = false, usage = "k of MS", metaVar = "1 2 3 4 5 6 7 8 9")
    private int msK = 7;

    public void appendLatexTables(String text) throws IOException {
        Path path = Paths.get(tfd_home).resolve("excels").resolve("tables.tex");
        Files.write(path, text.getBytes(StandardCharsets.US_ASCII), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public String getShortDescription() {
        return "Y Utility";
    }

    @Override
    public Path excelFile() throws IOException {

        Path excelPath = Paths.get(tfd_home).resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        StringBuilder file = new StringBuilder("Y");
        for (int i = 0; i < collections.length; i++) {
            file.append(collections[i].toString());
            if (i < collections.length - 1)
                file.append("_");
        }
        return excelPath.resolve(file.toString() + tag + optimize.toString() + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }

    private Map<String, Set<TFDAwareNeed>> decorate(Map<String, List<InfoNeed>> map, Map<DataSet, Decorator> decorators) {

        Map<String, Set<TFDAwareNeed>> decorated = new HashMap<>(map.size());

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {
            Set<TFDAwareNeed> needs = new HashSet<>(entry.getValue().size());
            for (InfoNeed need : entry.getValue()) {
                TFDAwareNeed tfdAwareNeed = decorators.get(need.dataSet()).tfdAwareNeed(need);
                if (needs.contains(tfdAwareNeed)) throw new RuntimeException("set shouldn't contain : " + need);
                else
                    needs.add(tfdAwareNeed);
            }
            decorated.put(entry.getKey(), needs);

        }

        return decorated;
    }

    private List<TFDAwareNeed> residualTFDAwareNeeds(List<InfoNeed> needs, Map<DataSet, Decorator> decorators) throws IOException {

        List<TFDAwareNeed> tfdAwareNeeds = new ArrayList<>(needs.size());

        for (InfoNeed need : needs) {
            TFDAwareNeed tfdAwareNeed = decorators.get(need.dataSet()).tfdAwareNeed(need);
            tfdAwareNeeds.add(tfdAwareNeed);
        }

        return tfdAwareNeeds;
    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet[] dataSets = new DataSet[collections.length];
        String[] evalDirs = new String[collections.length];
        Map<DataSet, Decorator> decorators = new HashMap<>(collections.length);

        for (int i = 0; i < collections.length; i++) {
            dataSets[i] = CollectionFactory.dataset(collections[i], tfd_home);
            evalDirs[i] = evalDirectory(dataSets[i], optimize);
            decorators.put(dataSets[i], new Decorator(dataSets[i], tag, freq));
        }

        System.out.println(Arrays.toString(dataSets));
        System.out.println(Arrays.toString(evalDirs));

        Evaluator evaluator = new Evaluator(dataSets, tag, optimize, "all", evalDirs, op);


        report = optimize;
        modelSet = evaluator.getModelSet();


        List<InfoNeed> residualNeeds = evaluator.residualNeeds(0);
        residualNeedsSize = residualNeeds.size();
        System.out.println("residual Size " + residualNeedsSize + " all " + evaluator.getNeeds().size() + " same =" + evaluator.getAllSame().size() + " zero = " + evaluator.getAllZero().size());
        List<TFDAwareNeed> testNeeds = residualTFDAwareNeeds(residualNeeds, decorators);


        RxT = runTopic("RxTx" + optimize.toString(), evaluator, residualNeeds, evaluator.randomMLEMap());


        List<Solution> accuracyList = evaluator.modelsAsSolutionList(residualNeeds);

        Solution bestSingle = Collections.max(accuracyList, Comparator.comparing(o -> o.sigma1));

        bestSingleScores = bestSingle.scores;

        Solution SGL = Collections.max(accuracyList, Comparator.comparing(o -> o.sigma1)).clone();
        SGL.model = "SGL";
        SGL.key = "SGL";
        SGL.predict = Predict.DIV;

        // accuracyList.add(oracleMin);
        // accuracyList.add(oracleMax);


        accuracyList.sort((Solution o1, Solution o2) -> Double.compare(o2.sigma1, o1.sigma1));
        System.out.println("Number of hits (percentage) : ");
        accuracyList.forEach(System.out::println);

        /**
         * T-statistics separate sheet, Run X Percentages for Best Single Run
         */
        Summary.createRow(0).createCell(1, CellType.STRING).setCellValue("T-stats");
        Summary.getRow(0).createCell(2, CellType.STRING).setCellValue("W-stats");
        Summary.getRow(0).createCell(3, CellType.STRING).setCellValue(optimize.toString());
        Summary.getRow(0).createCell(4, CellType.STRING).setCellValue("sigma1");
        Summary.getRow(0).createCell(5, CellType.STRING).setCellValue("sigma0");


        if (var == 0) {
            winnerMap = decorate(evaluator.bestModelMap, decorators);
            loserMap = decorate(evaluator.worstModelMap, decorators);
        } else {
            winnerMap = decorate(evaluator.filter(evaluator.bestModelMap, var), decorators);
            loserMap = decorate(evaluator.filter(evaluator.worstModelMap, var), decorators);
        }

        checkNoWinnerLoserCase();

        doSelectiveTermWeighting(testNeeds, evaluator);

        Solution MS = MSSolution(msK, testNeeds);
        if (MS != null)
            evaluator.calculateAccuracy(MS);

        Map<String, Double> geoRiskMap = addGeoRisk2Sheet(RxT);

        accuracyList.sort(Comparator.comparing(o -> o.key));

        accuracyList.forEach(this::writeSolution2SummarySheet);

        writeSolution2SummarySheet(oracleMax);
        writeSolution2SummarySheet(RND);
        writeSolution2SummarySheet(RMLE);


        accuracyList.add(oracleMax);
        accuracyList.add(RND);
        accuracyList.add(RMLE);
        accuracyList.add(SEL);
        if (MS != null)
            accuracyList.add(MS);

        accuracyList.forEach(solution -> {
            solution.geoRisk = geoRiskMap.get(solution.key);
        });


        appendLatexTables(latex(accuracyList));
        dumpRankInfo();

        RMLE.predict = Predict.DIV;
        // solutionList.add(RMLE);
        // solutionList.add(SGL);
        persistSolutionsLists();
    }

    private Solution MSSolution(int k, List<TFDAwareNeed> testNeeds) throws IOException, InvalidFormatException {

        Path MSExcelPath = MSExcelFile();
        if (MSExcelPath == null) return null;
        Workbook workbook = WorkbookFactory.create(MSExcelFile().toFile(), null, true);

        Sheet RxT = workbook.getSheet("RxTx" + optimize.toString());

        if (RxT == null) RxT = workbook.getSheet("RxTxNDCG100");

        Iterator<Row> iterator = RxT.rowIterator();

        Row r0 = iterator.next();

        int n = r0.getLastCellNum();

        for (int i = 2; i < n; i++) {
            String topic = r0.getCell(i).getStringCellValue();
            TFDAwareNeed need = testNeeds.get(i - 2);

            if (!topic.equals("T" + need.id()))
                throw new RuntimeException("excel topic header does not match with the test query");
        }


        while (iterator.hasNext()) {

            Row row = iterator.next();

            String model = row.getCell(1).getStringCellValue();

            if (!("MS" + k).equals(model))
                continue;

            List<Prediction> predictionList = new ArrayList<>();

            ++counter;
            this.RxT.createRow(counter).createCell(1).setCellValue("MS" + k);

            for (int i = 2; i < n; i++) {
                double predictedScore = row.getCell(i).getNumericCellValue();
                this.RxT.getRow(counter).createCell(i).setCellValue(predictedScore);
                TFDAwareNeed testQuery = testNeeds.get(i - 2);

                if (!r0.getCell(i).getStringCellValue().equals("T" + testQuery.id()))
                    throw new RuntimeException("excel topic header does not match with the test query");

                Prediction prediction = new Prediction(testQuery, null, predictedScore);
                predictionList.add(prediction);
            }

            Solution MS = new Solution(predictionList, k);
            MS.setKey("MS" + k);
            workbook.close();
            return MS;
        }

        workbook.close();
        return null;
    }

    protected Path MSExcelFile() throws IOException {

        if (collections.length != 1) {
            System.out.println("Model Selection only works for only single collection! " + Arrays.toString(collections));
            return null;
        }

        Path excelPath = Paths.get(tfd_home, collections[0].toString()).resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        return excelPath.resolve("MS" + collections[0] + optimize.toString() + tag + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }


    @Override
    protected String latexHeader() {
        String dataset;
        if (collections.length == 1)
            dataset = collections[0].toString();
        else if (collections.length == 2) {
            dataset = collections[0].toString() + collections[1].toString();
        } else
            dataset = Arrays.toString(collections);

        String anchor = "KStemAnchor".equals(tag) ? "Anchor" : "NoAnchor";

        String tableName = dataset + optimize.toString() + anchor;

        String header = header();
        return String.format(header, prettyDataSet(dataset), anchor, residualNeedsSize, optimize.toString(), tableName, optimize.toString());
    }


    public static void main(String[] args) {

        Collection[] collections = {Collection.CW09A, Collection.CW12B, Collection.GOV2, Collection.ROB04, Collection.MQ09};


        for (int i = 2; i <= collections.length; i++) {

            Generator.combination(collections)
                    .simple(i)
                    .stream()
                    .forEach(comb -> {
                        StringBuilder builder = new StringBuilder();

                        for (Collection col : comb)
                            builder.append(col).append("_");

                        builder.deleteCharAt(builder.length() - 1);
                        System.out.print(builder.toString() + " ");

                    });


        }

        double[] doubles = {1.2, 5.0, 0.1};

        RankingAlgorithm rankingAlgorithm = new NaturalRanking();

        System.out.println();
        System.out.println(Arrays.toString(rankingAlgorithm.rank(doubles)));
    }


}
