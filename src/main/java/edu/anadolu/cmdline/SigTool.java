package edu.anadolu.cmdline;

import edu.anadolu.exp.FullFactorial;
import edu.anadolu.knn.Measure;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import static edu.anadolu.knn.Measure.MAP;
import static edu.anadolu.knn.Measure.NDCG100;

/**
 * Significance Test for Model Selection (MS) versus Selective Term Weighting (SEL)
 */
public class SigTool extends CmdLineTool {

    @Option(name = "-k", metaVar = "[2 to 10]", required = true, usage = "Query ID")
    private int k = 7;

    String tfd_home;

    final TTest tTest = new TTest();
    final WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();
    private final static Measure[] measures = {NDCG100, MAP};

    private void check(Path path) {
        if (!Files.exists(path))
            System.out.println("does not exists! " + path.toString());
    }

    private double[] find(Path excelFile, String sheetName, String runName) throws IOException, InvalidFormatException {

        Workbook workbook = WorkbookFactory.create(excelFile.toFile(), null, true);

        Sheet RxT = workbook.getSheet(sheetName);

        Iterator<Row> iterator = RxT.rowIterator();

        Row r0 = iterator.next();

        int n = r0.getLastCellNum();

        //System.out.println(r0.getCell(n - 1).getStringCellValue());

        double[] array = new double[n - 2];

        while (iterator.hasNext()) {

            Row row = iterator.next();

            String run = row.getCell(1).getStringCellValue();
            if (!runName.equals(run)) continue;

            int counter = 0;
            // System.out.println(run);
            for (int i = 2; i < n; i++) {
                //  System.out.print(row.getCell(i).getNumericCellValue() + ",");
                array[counter++] = row.getCell(i).getNumericCellValue();
            }
        }
        workbook.close();

        return array;
    }

    @Override
    public void run(Properties props) throws Exception {

        final double alpha = 0.05;
        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;

        if (k < 2 || k > 10) {
            System.out.println("Invalid k option:" + k + "! Query ID must be between 2 and 10");
            return;
        }

        for (String tag : IPTool.tags)
            for (Measure measure : measures) {

                System.out.print("\t& " + measure + " & ");

                for (FullFactorial.EXP test : FullFactorial.experiments(tag)) {

                    // System.out.println(test.collection.toString() + measure.toString() + tag);
                    Path excelPath = Paths.get(tfd_home, test.collection.toString()).resolve("excels");


                    Path MS = excelPath.resolve("MS" + test.collection + measure.toString() + test.tag() + test.op.toUpperCase(Locale.ENGLISH) + ".xlsx");
                    check(MS);
                    Path SEL = excelPath.resolve("X" + test.collection.toString() + measure.toString() + test.tag() + test.collection.toString() + measure.toString() + test.op.toUpperCase(Locale.ENGLISH) + ".xlsx");
                    check(SEL);

                    double[] ms = find(MS, "RxTxNDCG100", "MS" + k);
                    double[] sel = find(SEL, "RxTx" + measure.toString(), "DIV_Cm");


                    if (StatUtils.mean(sel) >= StatUtils.mean(ms)) {
                        System.out.print("$\\uparrow$");
                    } else
                        System.out.print("$\\downarrow$");

                    double wP = wilcoxon.wilcoxonSignedRankTest(ms, sel, false);
                    if (wP < alpha)
                        //System.out.println("Wilcoxon " + wP);
                        System.out.print("$\\aleph$");

                    double tP = tTest.pairedTTest(ms, sel);
                    if (tP < alpha)
                        //System.out.println("TTest " + tP);
                        System.out.print("$\\dagger$");

                    System.out.print(" & ");
                }
                System.out.println("\\\\");
            }
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public String getShortDescription() {
        return "Significance Test for Model Selection (MS) versus Selective Term Weighting";
    }


}
