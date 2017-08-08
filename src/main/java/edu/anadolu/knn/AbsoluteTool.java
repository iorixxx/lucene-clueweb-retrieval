package edu.anadolu.knn;

import edu.anadolu.Decorator;
import edu.anadolu.cmdline.SpamEvalTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.freq.Freq;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Absolute wins for term-weighting models
 */
public class AbsoluteTool extends KNNTool {

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

        Path excelFile = excelPath.resolve("Absolute" + collection.toString() + tag + measure + op.toUpperCase(Locale.ENGLISH) + ".xlsx");


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

        Map<String, List<InfoNeed>> bestModelMap = evaluator.absoluteBestModelMap();
        bestModelMap.remove("ALL_SAME");
        bestModelMap.remove("ALL_ZERO");

        Decorator decorator = new Decorator(dataset, tag, Freq.Rel);

        QuerySimilarity querySimilarity = new CartesianQueryTermSimilarity(new UnEqualDataPoints(false, false), false, CartesianQueryTermSimilarity.Aggregation.Euclid, CartesianQueryTermSimilarity.Way.s);

        for (Map.Entry<String, List<InfoNeed>> entry : bestModelMap.entrySet()) {
            String model = entry.getKey();

            Sheet sheet = workbook.createSheet(model);

            List<InfoNeed> absoluteWinners = entry.getValue();

            List<TFDAwareNeed> absoluteTFDAwareNeeds = decorator.residualTFDAwareNeeds(absoluteWinners);
            int rowNum = 0;

            Row r0 = sheet.createRow(rowNum);

            r0.createCell(0).setCellValue("qID");
            r0.createCell(1).setCellValue("term");
            r0.createCell(2).setCellValue("Zero");

            for (int bin = 1; bin <= 1000; bin++) {
                // freqSheet.addCell(new Label(bin + 2, 0, "Bin" + bin));
                r0.createCell(bin + 2).setCellValue("Bin" + bin);
            }
            rowNum++;

            for (TFDAwareNeed need : absoluteTFDAwareNeeds) {

                int i = 1;

                for (Double[] l : need.termFreqDistNormalized) {
                    Row row = sheet.createRow(rowNum);
                    row.createCell(0, CellType.NUMERIC).setCellValue(need.id());
                    row.createCell(1, CellType.STRING).setCellValue(need.getPartOfQuery(i++));

                    int colNum = 2;
                    double cdf = 0.0;
                    for (Double ll : l) {
                        cdf += ll;
                        row.createCell(colNum++, CellType.NUMERIC).setCellValue(cdf);
                    }

                    rowNum++;
                }
            }

            /*
             * Characteristic Distribution for the model
             */
            Row row = sheet.createRow(rowNum);
            row.createCell(0, CellType.STRING).setCellValue(model + "_" + "char");
            row.createCell(1, CellType.STRING).setCellValue("AVERAGE");

            int columnNumber = 2;

            for (Double d : absoluteTFDAwareNeeds.get(0).termFreqDistZeroNormalized.get(0)) {
                String columnLetter = CellReference.convertNumToColString(columnNumber);
                Cell cell = row.createCell(columnNumber, CellType.NUMERIC);
                cell.setCellType(CellType.FORMULA);
                cell.setCellFormula("AVERAGE(" + columnLetter + "2:" + columnLetter + Integer.toString(rowNum) + ")");
                columnNumber++;
            }


            sheet = workbook.createSheet("Q2Q_" + model);

            // Q2QTool.addTopicHeaders(sheet, absoluteTFDAwareNeeds);

            //  Q2QTool.fillSheetWithChiSquare(sheet, absoluteTFDAwareNeeds, querySimilarity);

        }

        workbook.write(Files.newOutputStream(excelFile));
        workbook.close();
    }

    static Double[] cdf(Double[] R) {

        Double[] ret = new Double[R.length];

        double cdf = 0.0;
        for (int i = 0; i < R.length; i++) {
            cdf += R[i];
            ret[i] = cdf;
        }

        return ret;
    }
}
