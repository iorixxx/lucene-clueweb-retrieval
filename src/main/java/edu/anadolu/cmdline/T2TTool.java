package edu.anadolu.cmdline;

import edu.anadolu.Decorator;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.Freq;
import edu.anadolu.knn.ChiBase;
import edu.anadolu.knn.ChiSquare;
import edu.anadolu.knn.TFDAwareNeed;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Term2Term (T2T Tool)
 */
class T2TTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Term2Term (T2T Tool)";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
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

        Path collectionPath = Paths.get(tfd_home, collection.toString());

        Path excelPath = collectionPath.resolve("excels");
        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        Map<String, String> map = new HashMap<>();

        for (String tag : tags) {
            if ("KStemAnalyzerAnchor".equals(tag) && (Collection.GOV2.equals(collection) || Collection.ROB04.equals(collection)))
                continue;

            Workbook workbook = new XSSFWorkbook();
            Path excelFile = excelPath.resolve("T2T" + dataset.collection().toString() + tag + ".xlsx");


            for (Freq type : types) {
                Decorator decorator = new Decorator(dataset, tag, type);
                map = new HashMap<>();

                for (InfoNeed need : decorator.allQueries) {
                    map.putAll(decorator.getFrequencyDistributionList(need, type.fileName(1000)));
                }


                for (boolean zero : new boolean[]{true})
                    for (boolean cdf : new boolean[]{false}) {

                        final String sheetName = decorator.type() + (zero ? "Z" : "") + (cdf ? "c" : "p");

                        Sheet sheet = workbook.createSheet(sheetName);

                        addTopicHeaders(sheet, map.keySet());


                        ChiBase chi = new ChiSquare(false, cdf);


                        int r = 1;
                        for (String term : map.keySet()) {

                            Row row = sheet.getRow(r);

                            int c = 1;
                            for (String other : map.keySet()) {


                                Double[] first = normalize(zero ? decorator.parseFreqLine(decorator.addZeroColumnToLine(map.get(term))) : decorator.parseFreqLine(map.get(term)));
                                Double[] second = normalize(zero ? decorator.parseFreqLine(decorator.addZeroColumnToLine(map.get(other))) : decorator.parseFreqLine(map.get(other)));


                                final double similarity = chi.chiSquared(first, second);

                                row.createCell(c, CellType.NUMERIC).setCellValue(similarity);

                                c++;
                            }

                            r++;
                        }

                    }
            }


            workbook.write(Files.newOutputStream(excelFile));
            workbook.close();
        }

        System.out.print("terms={");
        for (String term : map.keySet()) {
            System.out.print("'" + term + "',");
        }
        System.out.println("}");
    }

    private static void addTopicHeaders(Sheet sheet, Set<String> set) {
        Row r0 = sheet.createRow(0);

        int c = 1;
        for (String term : set) {
            r0.createCell(c, CellType.STRING).setCellValue(term);
            c++;
        }

        int r = 1;
        for (String term : set) {
            Row row = sheet.createRow(r);
            row.createCell(0, CellType.STRING).setCellValue(term);
            r++;
        }

    }

    private static Double[] normalize(Long[] a) {

        long df = TFDAwareNeed.df(a);
        final Double[] array = new Double[a.length];
        for (int i = 0; i < a.length; i++)
            array[i] = (double) a[i] / df;

        return array;

    }
}
