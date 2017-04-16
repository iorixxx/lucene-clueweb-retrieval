package edu.anadolu.cmdline;

import edu.anadolu.ModelSelection;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Solution;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Model Selection (MS) Tool
 */
final class MSTool extends EvaluatorTool {

    @Override
    public String getShortDescription() {
        return "Tool for Model Selection (MS) framework";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    protected String evalDirectory(DataSet dataset) {
        if (Collection.GOV2.equals(dataset.collection()) || Collection.MC.equals(dataset.collection()) || Collection.ROB04.equals(dataset.collection())) {
            return "evals";
        } else {
            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, op);
            return bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }
    }

    private XSSFWorkbook workbook;
    private Sheet Summary = null;
    private Sheet RxT = null;

    MSTool() {
        workbook = new XSSFWorkbook();
        Summary = workbook.createSheet("Summary");
        RxT = workbook.createSheet("RxTx" + measure.toString());

        Summary.createRow(0).createCell(1, CellType.STRING).setCellValue("sigma0");
        Summary.getRow(0).createCell(2, CellType.STRING).setCellValue("sigma1");
        Summary.getRow(0).createCell(3, CellType.STRING).setCellValue(measure.toString());
    }

    private int counter;

    private void runTopic() {
        /**
         * Insert Run X Topic Matrix as a separate sheet
         */

        List<String> models = new ArrayList<>(evaluator.getModelSet());
        Collections.sort(models);

        Row r0 = RxT.createRow(0);
        r0.createCell(1).setCellValue("Model");

        counter = 0;
        for (String model : models) {

            Row r = RxT.createRow(++counter);
            r.createCell(1).setCellValue(model);

        }


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

        }
    }

    private void writeSolution(Solution solution) {

        Row r = RxT.createRow(++counter);
        r.createCell(1).setCellValue("MS" + solution.k);

        int c = 1;
        for (Double score : solution.scores()) {
            c++;
            r.createCell(c).setCellValue(score);
        }

    }

    Evaluator evaluator;
    private List<InfoNeed> residualNeeds;
    String tfd_home;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory(dataset), op);

        residualNeeds = evaluator.residualNeeds();

        List<Solution> modelsAsSolutionList = evaluator.modelsAsSolutionList(residualNeeds);
        modelsAsSolutionList.sort((o1, o2) -> (int) Math.signum(o1.getMean() - o2.getMean()));
        Collections.reverse(modelsAsSolutionList);
        modelsAsSolutionList.forEach(System.out::println);

        ModelSelection modelSelection = new ModelSelection(dataset, tag);
        Solution max = modelSelection.evaluate(evaluator);
        System.out.println(max);
        evaluator.printMeanL(residualNeeds);
        evaluator.printMean();
        //System.out.println(solution.k + "\t" + String.format("%.2f", solution.sigma1) + "\t" + String.format("%.5f", solution.getMean()));
        modelSelection.printSolutionList();

        runTopic();
        List<Solution> solutionList = modelSelection.solutionList();
        solutionList.forEach(this::writeSolution);


        for (Solution solution : solutionList)
            System.err.println("MS," + solution.k + "," + collection.toString() + "," + measure.toString() + "," + String.format("%.5f", solution.sigma1) + "," + tag);


        for (int i = 0; i < solutionList.size(); i++) {
            Row row = Summary.createRow(i + 1);
            Solution solution = solutionList.get(i);
            row.createCell(0, CellType.STRING).setCellValue(solution.model);
            row.createCell(1, CellType.NUMERIC).setCellValue(solution.sigma0);
            row.createCell(2, CellType.NUMERIC).setCellValue(solution.sigma1);
            row.createCell(3, CellType.NUMERIC).setCellValue(solution.getMean());
        }

        workbook.write(Files.newOutputStream(excelFile()));
        workbook.close();
    }

    protected Path excelFile() throws IOException {

        Path excelPath = Paths.get(tfd_home, collection.toString()).resolve("excels");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        return excelPath.resolve("MS" + collection + measure.toString() + tag + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }
}
