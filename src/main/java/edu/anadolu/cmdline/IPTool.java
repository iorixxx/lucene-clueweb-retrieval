package edu.anadolu.cmdline;

import edu.anadolu.exp.FullFactorial;
import edu.anadolu.knn.Measure;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.knn.Measure.*;

/**
 * Interaction Plot (IP) Tool
 */
class IPTool extends CmdLineTool {

    static final List<String> tags = Arrays.asList("Anchor", "NoAnchor");

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Override
    public String getShortDescription() {
        return "Interaction Plot (IP) Utility";
    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if ("rank".equals(task)) {
            rankSummary(0);
            rankSummary(1);
            return;
        }

        if ("summary".equals(task)) {
            for (String tag : tags) {
                summary(tag);
                summaryCross(tag);
            }

            append("summary");
            append("summaryCross");
            return;
        }

        String[] sheetNames = {"IPxAxCar", "IPxA"};

        for (String sheetName : sheetNames) {

            if ("full".equals(task)) {
                ip(sheetName);
                ipCross(sheetName);
            } else if ("short".equals(task)) {

                for (String tag : tags) {
                    ip(sheetName, NDCG100, NDCG100, tag);
                    ipCross(sheetName, NDCG100, NDCG100, tag);

                    //  ip(sheetName, NDCG20, NDCG20, tag);
                    //  ipCross(sheetName, NDCG20, NDCG20, tag);

                    ip(sheetName, MAP, MAP, tag);
                    ipCross(sheetName, MAP, MAP, tag);
                }
            }
        }
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    private String tfd_home;

    private final static Measure[] measures = {NDCG20, NDCG100, ERR20, ERR100, MAP};

    private Path excelFile(FullFactorial.EXP test, FullFactorial.EXP train, Measure optimize, Measure report) {

        Path collectionPath = Paths.get(tfd_home, test.collection.toString());
        Path excelPath = collectionPath.resolve("excels");
        if (!Files.exists(excelPath))
            throw new RuntimeException(excelPath + " does not exist!");

        if (!test.tag().equals(train.tag()))
            throw new RuntimeException("tags does not match!");

        final Path excelFile = excelPath.resolve("X" + test.collection.toString() + report.toString() + test.tag() + train.collection.toString() + optimize.toString() + test.op.toUpperCase(Locale.ENGLISH) + ".xlsx");

        if (!Files.isRegularFile(excelFile))
            throw new RuntimeException("cannot find : " + excelFile.toString());

        if (!Files.exists(excelFile))
            throw new RuntimeException("cannot find : " + excelFile.toString());

        return excelFile;
    }

    private void inter(FullFactorial.EXP test, FullFactorial.EXP train, Measure optimize, Measure report, PrintWriter output, String sheetName, boolean first) throws Exception {
        Path excelFile = excelFile(test, train, optimize, report);

        System.out.println(excelFile.getFileName().toString());

        XSSFWorkbook workbook = new XSSFWorkbook(excelFile.toFile());

        Sheet IPxA = workbook.getSheet(sheetName);

        Iterator<Row> iterator = IPxA.rowIterator();

        Row r0 = iterator.next();

        int n = r0.getLastCellNum() - 1;

        if (first) {

            for (int i = 0; i < n; i++) {
                output.print(r0.getCell(i).getStringCellValue() + ",");
            }
            output.println(r0.getCell(n).getStringCellValue());
        }

        while (iterator.hasNext()) {

            Row row = iterator.next();

            for (int i = 0; i < n; i++) {
                output.print(row.getCell(i).getStringCellValue() + ",");
            }

            output.println(row.getCell(n).getNumericCellValue());

        }
        workbook.close();
    }

    private void ipCross(String sheetName) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", sheetName + "Cross.csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;

        for (Measure optimize : measures)
            for (Measure report : measures)
                for (FullFactorial.EXP test : FullFactorial.experiments("NoAnchor")) {

                    for (FullFactorial.EXP train : FullFactorial.experiments("NoAnchor")) {

                        inter(test, train, optimize, report, output, sheetName, first);

                        if (first) {
                            first = false;
                        }
                    }
                }

        output.flush();
        output.close();
    }

    private void ipCross(String sheetName, Measure optimize, Measure report, String tag) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", sheetName + "Cross" + optimize.toString() + report.toString() + tag + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;


        for (FullFactorial.EXP test : FullFactorial.experiments(tag)) {

            for (FullFactorial.EXP train : FullFactorial.experiments(tag)) {

                inter(test, train, optimize, report, output, sheetName, first);

                if (first) {
                    first = false;
                }
            }
        }

        output.flush();
        output.close();
    }

    private void ip(String sheetName) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", sheetName + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;

        for (Measure optimize : measures)
            for (Measure report : measures)
                for (FullFactorial.EXP exp : FullFactorial.experiments("NoAnchor")) {

                    inter(exp, exp, optimize, report, output, sheetName, first);

                    if (first) {
                        first = false;
                    }
                }

        output.flush();
        output.close();
    }

    private void ip(String sheetName, Measure optimize, Measure report, String tag) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", sheetName + optimize.toString() + report.toString() + tag + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;

        for (FullFactorial.EXP exp : FullFactorial.experiments(tag)) {

            inter(exp, exp, optimize, report, output, sheetName, first);

            if (first) {
                first = false;
            }
        }

        output.flush();
        output.close();
    }

    private void append(String summary) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", summary + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));


        List<String> lines = Files.readAllLines(Paths.get(tfd_home, "excels", summary + tags.get(0) + ".csv"));
        String header = lines.get(0) + ",AnchorText";

        output.println(header);

        for (int i = 1; i < lines.size(); i++)
            output.println(lines.get(i) + "," + tags.get(0));

        lines = Files.readAllLines(Paths.get(tfd_home, "excels", summary + tags.get(1) + ".csv"));
        for (int i = 1; i < lines.size(); i++)
            output.println(lines.get(i) + "," + tags.get(1));

        output.flush();
        output.close();

    }

    private void summary(String tag) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", "summary" + tag + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;

        for (FullFactorial.EXP exp : FullFactorial.experiments(tag)) {

            inter(exp, exp, MAP, MAP, output, "IPxA", first);
            inter(exp, exp, NDCG100, NDCG100, output, "IPxA", false);

            if (first) {
                first = false;
            }
        }

        output.flush();
        output.close();
    }

    private void summaryCross(String tag) throws Exception {

        Path IPPath = Paths.get(tfd_home, "excels", "summaryCross" + tag + ".csv");

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(IPPath, StandardCharsets.US_ASCII));

        boolean first = true;

        for (FullFactorial.EXP test : FullFactorial.experiments(tag))

            for (FullFactorial.EXP train : FullFactorial.experiments(tag)) {

                inter(test, train, MAP, MAP, output, "IPxA", first);
                inter(test, train, NDCG100, NDCG100, output, "IPxA", false);

                if (first) {
                    first = false;
                }
            }

        output.flush();
        output.close();
    }

    private void rankSummary(int col) throws IOException, InvalidFormatException {

        Measure[] measures = {NDCG100, MAP};
        String[] sets = {"CW09A_CW12B", "MQ09", "CW09A", "CW09B", "CW12B"};
        String[] pSets = {"CW\\{09A\\textbar12B\\}", "MQ09", "CW09A", "CW09B", "CW12B"};

        System.out.print(" & ");
        for (String set : pSets)
            System.out.print(" & " + set);
        System.out.println(" \\\\");


        for (String tag : new String[]{"KStemAnchor", "KStem"}) {

            String anchor = "KStemAnchor".equals(tag) ? "Anchor" : "NoAnchor";

            System.out.println("\\multirow{2}{*}{" + anchor + "}");

            for (Measure measure : measures) {
                System.out.print("\t& " + measure);

                for (String set : sets) {
                    Path excelFile = Paths.get(tfd_home, "excels", "Y" + set + tag + measure + "OR.xlsx");

                    XSSFWorkbook workbook = new XSSFWorkbook(excelFile.toFile());

                    Sheet rankInfo = workbook.getSheet("RankInfo");
                    Row row = rankInfo.getRow(0);
                    String rank = row.getCell(col).getStringCellValue();
                    System.out.print(" & " + rank);

                }
                System.out.println(" \\\\");
            }

            System.out.println("\\hline");
        }
    }
}
