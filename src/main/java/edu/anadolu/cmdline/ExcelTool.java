package edu.anadolu.cmdline;

import edu.anadolu.eval.Metric;
import edu.anadolu.exp.FullFactorial;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Excel Tool: Summary k-NN
 */
public class ExcelTool extends CmdLineTool {

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Metric metric = Metric.NDCG;

    @Option(name = "-k", metaVar = "[20|100|1000]", required = false, usage = "Non-negative integer depth of ranking to evaluate")
    protected int k = 100;

    @Override
    public String getShortDescription() {
        return "Excel Utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    protected String tfd_home;

    private final String[] sheets = {"RxPxT-Best-Phi", "RxPxW-Best-Phi", "RxTxP", "RxAxP"};

    List<String> runNames = null;

    public void setRunNames(Sheet sheet) {

        if (runNames == null) {
            runNames = getRunNames(sheet);
            return;
        }

        List<String> runs = getRunNames(sheet);

        if (runs.size() != runNames.size())
            throw new RuntimeException("run name sizes differ!");

        for (int i = 0; i < runNames.size(); i++) {
            if (!runs.get(i).equals(runNames.get(i)))
                throw new RuntimeException("elements differ! " + runs.get(i) + " " + runNames.get(i));
        }
    }

    public List<String> getRunNames(Sheet sheet) {
        int rowStart = sheet.getFirstRowNum();
        int rowEnd = sheet.getLastRowNum();

        List<String> runNames = new ArrayList<>();
        for (int rowNum = rowStart + 1; rowNum <= rowEnd; rowNum++) {
            Row r = sheet.getRow(rowNum);
            runNames.add(r.getCell(1).getStringCellValue());
        }
        return runNames;
    }

    /**
     * Resolves name of the excel file, where results are stored, given an experiment.
     *
     * @param exp
     * @return
     */
    protected Path excelFile(FullFactorial.EXP exp) {

        Path collectionPath = Paths.get(tfd_home, exp.collection.toString());
        Path excelPath = collectionPath.resolve("excels");
        if (!Files.exists(excelPath))
            throw new RuntimeException(excelPath + " does not exist!");

        return excelPath.resolve("X" + exp.collection.toString() + exp.tag() + metric.toString() + Integer.toString(k) + exp.op.toUpperCase(Locale.ENGLISH) + ".xlsx");

//        final Path excelFile;
//
//        if (Collection.MQ09.equals(exp.collection)) {
//            excelFile = excelPath.resolve("X" + exp.collection.toString() + exp.tag() + Metric.statAP + "1000" + exp.op.toUpperCase(Locale.ENGLISH) + ".xlsx");
//        } else
//            excelFile = excelPath.resolve("X" + exp.collection.toString() + exp.tag() + metric.toString() + Integer.toString(k) + exp.op.toUpperCase(Locale.ENGLISH) + ".xlsx");
//
//        return excelFile;
    }

    Map<String, List<Double>> geoRisk(FullFactorial.EXP exp) throws Exception {

        Map<String, List<Double>> geoRiskMap = new HashMap<>(sheets.length);

        Path excelFile = excelFile(exp);

        XSSFWorkbook workbook = new XSSFWorkbook(excelFile.toFile());

        for (String type : sheets) {

            Sheet sheet = workbook.getSheet(type);

            setRunNames(sheet);

            Row r0 = sheet.getRow(0);
            //For zeroth row, iterate through each columns
            Iterator<Cell> cellIterator = r0.cellIterator();

            int c = 0;
            while (cellIterator.hasNext()) {

                Cell cell = cellIterator.next();

                if ("geoRisk".equals(cell.getStringCellValue())) break;

                c++;
            }

            c++;

            int rowStart = sheet.getFirstRowNum();
            int rowEnd = sheet.getLastRowNum();

            List<Double> doubleList = new ArrayList<>();
            for (int rowNum = rowStart + 1; rowNum <= rowEnd; rowNum++) {
                Row r = sheet.getRow(rowNum);
                doubleList.add(r.getCell(c).getNumericCellValue());
            }

            geoRiskMap.put(type, doubleList);

        }

        workbook.close();
        return geoRiskMap;
    }

    /**
     * @param exp
     * @return average effectiveness over all percentages
     * @throws Exception
     */
    List<Double> averageSumStats(FullFactorial.EXP exp, String sheetName) throws Exception {


        Path excelFile = excelFile(exp);

        XSSFWorkbook workbook = new XSSFWorkbook(excelFile.toFile());


        Sheet sheet = workbook.getSheet(sheetName);

        setRunNames(sheet);

        Row r0 = sheet.getRow(0);
        //For zeroth row, iterate through each columns
        Iterator<Cell> cellIterator = r0.cellIterator();

        int colStart = -1;
        int colEnd = -1;
        int c = 0;
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            c++;
            if ("10P".equals(cell.getStringCellValue())) colStart = c;
            if ("90P".equals(cell.getStringCellValue())) colEnd = c;

        }

        if (colEnd == -1 || colStart == -1) throw new RuntimeException("cannot determine percentage column ranges");

        int rowStart = sheet.getFirstRowNum();
        int rowEnd = sheet.getLastRowNum();

        List<Double> doubleList = new ArrayList<>();
        for (int rowNum = rowStart + 1; rowNum <= rowEnd; rowNum++) {
            Row r = sheet.getRow(rowNum);
            double sum = 0.0;
            for (int colNum = colStart; colNum <= colEnd; colNum++)
                sum += r.getCell(colNum).getNumericCellValue();

            doubleList.add(sum / (colEnd - colStart + 1));
        }


        workbook.close();
        return doubleList;
    }

    public void addHeader(Sheet sheet) {
        Row r0 = sheet.createRow(0);
        r0.createCell(0, CellType.STRING).setCellValue("Run Name");
        for (FullFactorial.EXP exp : FullFactorial.experiments("Anchor")) {
            r0.createCell(exp.id, CellType.STRING).setCellValue("EXP" + exp.id);
        }
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }


        Path summaryExcelFile = Paths.get(tfd_home, "Summary.xlsx");

        Workbook workbook = new XSSFWorkbook();

        for (String type : sheets) {
            addHeader(workbook.createSheet(type));
        }

        addHeader(workbook.createSheet("Average"));
        addHeader(workbook.createSheet("Accuracy"));

        for (FullFactorial.EXP exp : FullFactorial.experiments("Anchor")) {

            System.out.println(exp.toString());

            Map<String, List<Double>> geoRiskMap = geoRisk(exp);

            for (String type : sheets) {

                Sheet sheet = workbook.getSheet(type);

                List<Double> doubleList = geoRiskMap.get(type);

                writeDoubleList(sheet, doubleList, exp);

                insertRunNames(sheet);
            }

            Sheet sheet = workbook.getSheet("Average");
            List<Double> doubleList = averageSumStats(exp, "RxTxP");
            writeDoubleList(sheet, doubleList, exp);
            insertRunNames(sheet);

            sheet = workbook.getSheet("Accuracy");
            doubleList = averageSumStats(exp, "RxAxP");
            writeDoubleList(sheet, doubleList, exp);
            insertRunNames(sheet);

        }

        workbook.write(Files.newOutputStream(summaryExcelFile));
        workbook.close();

    }

    public void writeDoubleList(Sheet sheet, List<Double> doubleList, FullFactorial.EXP exp) {
        for (int i = 0; i < doubleList.size(); i++) {
            int r = i + 1;
            if (sheet.getRow(r) == null) sheet.createRow(r);
            sheet.getRow(r).createCell(exp.id).setCellValue(doubleList.get(i));
        }
    }

    public void insertRunNames(Sheet sheet) {
        /**
         * Insert Run Names into zeroth column.
         */
        for (int i = 0; i < runNames.size(); i++) {
            int r = i + 1;
            if (sheet.getRow(r) == null) sheet.createRow(r);
            sheet.getRow(r).createCell(0, CellType.STRING).setCellValue(runNames.get(i));
        }
    }
}
