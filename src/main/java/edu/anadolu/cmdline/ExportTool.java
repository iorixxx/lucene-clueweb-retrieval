package edu.anadolu.cmdline;

import edu.anadolu.Decorator;
import edu.anadolu.Exporter;
import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.Freq;
import edu.anadolu.knn.TFDAwareNeed;
import edu.anadolu.stats.TermStats;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Export Tool: Saves run topic matrix to export and knn folders
 */
final class ExportTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Override
    public String getShortDescription() {
        return "Export Utility";
    }


    private Workbook workbook;

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


        for (String tag : tags) {
            if ("KStemAnchor".equals(tag) && (Collection.GOV2.equals(collection) || Collection.ROB04.equals(collection)))
                continue;


            QuerySelector selector = new QuerySelector(dataset, tag);

            Path excelFile = excelPath.resolve(dataset.collection().toString() + "TermFreqDist" + tag + ".xlsx");

            workbook = new XSSFWorkbook();


            Sheet statSheet = workbook.createSheet("TermStats");

            Row r0 = statSheet.createRow(0);

            r0.createCell(0).setCellValue("qID");
            r0.createCell(1).setCellValue("term");
            r0.createCell(2).setCellValue("TF");
            r0.createCell(3).setCellValue("DF");
            r0.createCell(4).setCellValue("CTI");
            r0.createCell(5).setCellValue("AVDL");
            r0.createCell(6).setCellValue("VARDL");
            r0.createCell(7).setCellValue("docCount");
            r0.createCell(8).setCellValue("termCount");

            int row = 1;
            for (InfoNeed need : selector.allQueries) {

                List<String> analyzedTokens = Analyzers.getAnalyzedTokens(need.query(), selector.analyzer());

                for (String word : analyzedTokens) {


                    TermStats termStats = selector.termStatisticsMap.get(word);

                    Row r = statSheet.createRow(row);

                    r.createCell(0).setCellValue(need.id());
                    r.createCell(1).setCellValue(word);
                    r.createCell(2).setCellValue(termStats.totalTermFreq());
                    r.createCell(3).setCellValue(termStats.docFreq());
                    r.createCell(4).setCellValue(termStats.cti());
                    r.createCell(5).setCellValue(termStats.avdl());
                    r.createCell(6).setCellValue(termStats.vardl());
                    r.createCell(7).setCellValue(selector.numberOfDocuments);
                    r.createCell(8).setCellValue(selector.numberOfTokens);

                    row++;
                }

            }


            write(new Decorator(dataset, tag, Freq.Rel, 1000), true);

          //  Exporter exporter = new Exporter(dataset, tag);
          //  Sheet qRel = workbook.createSheet("qRel");
          //  exporter.saveDistOverQRels(selector.allQueries, qRel);

            workbook.write(Files.newOutputStream(excelFile));
            workbook.close();


        }

    }

    public void write(Long[] array, Row r, boolean pdf) {
        int col = 2;
        double cdf = 0.0;
        for (double d : array) {
            cdf += d;
            if (pdf)
                r.createCell(col++).setCellValue(d);
            else
                r.createCell(col++).setCellValue(cdf);
        }
    }

    public void write(Double[] array, Row r, boolean pdf) {
        int col = 2;
        double cdf = 0.0;
        for (double d : array) {
            cdf += d;
            if (pdf)
                r.createCell(col++).setCellValue(d);
            else
                r.createCell(col++).setCellValue(cdf);
        }
    }

    public void write(Decorator decorator, boolean pdf) throws IOException {

        java.util.Collection<TFDAwareNeed> tfdAwareNeeds = decorator.residualTFDAwareNeeds();

        final String freqDistSheetName = (pdf ? "p" : "c") + decorator.type() + "FreqDist" + decorator.numBins();

        Sheet freqDist = workbook.createSheet(freqDistSheetName);
        Row r0 = freqDist.createRow(0);

        r0.createCell(0).setCellValue("qID");
        r0.createCell(1).setCellValue("term");

        for (int bin = 1; bin <= decorator.numBins() + 1; bin++) {
            r0.createCell(bin + 1).setCellValue("Bin" + bin);
        }

        int row = 1;
        for (TFDAwareNeed tfdAwareNeed : tfdAwareNeeds) {

            for (String word : tfdAwareNeed.distinctSet) {
                Double[] array = tfdAwareNeed.termFreqDistNormalized(word, decorator.analyzer());

                Row r = freqDist.createRow(row);

                r.createCell(0).setCellValue(tfdAwareNeed.id());
                r.createCell(1).setCellValue(word);

                write(array, r, pdf);

                row++;
            }
        }

        if (!pdf) return;

        final String freqDistZeroSheetName = (pdf ? "p" : "c") + decorator.type() + "FreqDistZero";
        Sheet freqDistZero = workbook.createSheet(freqDistZeroSheetName);
        r0 = freqDistZero.createRow(0);

        r0.createCell(0).setCellValue("qID");
        r0.createCell(1).setCellValue("term");
        r0.createCell(2).setCellValue("Zero");

        Sheet raw = workbook.createSheet("Raw");
        Row rawRow = raw.createRow(0);
        rawRow.createCell(0).setCellValue("qID");
        rawRow.createCell(1).setCellValue("term");
        rawRow.createCell(2).setCellValue("Zero");

        for (int bin = 1; bin <= decorator.numBins() + 1; bin++) {
            r0.createCell(bin + 2).setCellValue("Bin" + bin);
            rawRow.createCell(bin + 2).setCellValue("Bin" + bin);
        }

        row = 1;
        for (TFDAwareNeed tfdAwareNeed : tfdAwareNeeds) {


            for (String word : tfdAwareNeed.distinctSet) {
                Double[] array = tfdAwareNeed.termFreqDistZeroNormalized(word, decorator.analyzer());

                Row r = freqDistZero.createRow(row);

                r.createCell(0).setCellValue(tfdAwareNeed.id());
                r.createCell(1).setCellValue(word);

                write(array, r, pdf);

                Long[] longs = tfdAwareNeed.termFreqDistZeroRaw(word, decorator.analyzer());
                rawRow = raw.createRow(row);
                rawRow.createCell(0).setCellValue(tfdAwareNeed.id());
                rawRow.createCell(1).setCellValue(word);

                write(longs, rawRow, pdf);

                row++;
            }
        }
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }
}
