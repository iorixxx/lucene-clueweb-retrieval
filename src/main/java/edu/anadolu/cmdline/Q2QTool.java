package edu.anadolu.cmdline;

import edu.anadolu.Decorator;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.freq.Freq;
import edu.anadolu.knn.*;
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
import java.util.*;

/**
 * Query2Query (Q2Q Tool)
 */
class Q2QTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Query2Query (Q2Q Tool)";
    }

    private Map<InfoNeed, Set<String>> bestModelMap;

    private Map<String, int[]> colorMap = ColorUtil.colorMap();

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

            final String evalDirectory;

            if (Collection.GOV2.equals(collection) || Collection.MC.equals(collection) || Collection.ROB04.equals(collection)) {
                evalDirectory = "evals";
            } else {
                final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, Measure.NDCG100, "OR");
                evalDirectory = bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
            }

            System.out.println("Instantiating Evaluator with evaluation directory of " + evalDirectory);

            Evaluator evaluator = new Evaluator(dataset, tag, Measure.NDCG100, "all", evalDirectory, "OR");

            final Set<InfoNeed> allZero = evaluator.getAllZero();
            final Set<InfoNeed> allSame = evaluator.getAllSame();

            List<InfoNeed> infoNeedList = dataset.getTopics();

            List<InfoNeed> residualNeeds = new ArrayList<>(infoNeedList);
            residualNeeds.removeAll(allSame);
            residualNeeds.removeAll(allZero);


            bestModelMap = evaluator.singleLabelMap();


            Decorator decorator = new Decorator(dataset, tag, Freq.Rel);

            Workbook workbook = new XSSFWorkbook();
            Path excelFile = excelPath.resolve("Q2Q" + decorator.type() + dataset.collection().toString() + tag + ".xlsx");


            java.util.Collection<TFDAwareNeed> TFDAwareNeeds = decorator.residualTFDAwareNeeds(residualNeeds);

            addColors(workbook.createSheet("colors"), TFDAwareNeeds);

            for (final QuerySimilarity querySimilarity : KNNTool.querySimilarities()) {

                String key = querySimilarity.toString();
                Sheet sheet;
                try {
                    sheet = workbook.createSheet(key);
                } catch (java.lang.IllegalArgumentException e) {
                    sheet = workbook.createSheet(key + "1");
                }


                addTopicHeaders(sheet, TFDAwareNeeds);
                fillSheetWithChiSquare(sheet, TFDAwareNeeds, querySimilarity, Freq.Rel);


            }


            workbook.write(Files.newOutputStream(excelFile));
            workbook.close();
        }
    }


    /**
     * Return absolute winner model of the given query, if exists.
     *
     * @param need the query
     * @return best model for the query
     */

    private String winner(InfoNeed need) {

        if (!bestModelMap.containsKey(need))
            throw new RuntimeException("best model map does not contain " + need.toString());

        Set<String> set = bestModelMap.get(need);

        if (set.isEmpty()) throw new RuntimeException("winner set is empty for " + need.toString());

        if (set.size() == 1) return Evaluator.prettyModel(set.iterator().next());

        int size = set.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for (String obj : set) {
            if (i == item)
                return Evaluator.prettyModel(obj);
            i = i + 1;
        }

        throw new RuntimeException("cannot happen!");

    }

    private static void fillSheetWithChiSquare(Sheet sheet, java.util.Collection<TFDAwareNeed> TFDAwareNeeds, QuerySimilarity querySimilarity, Freq type) {

        int mostSimilarQueryShouldBeItSelf = 0;


        List<TFDAwareNeed> mostSimilarIsNotItself = new ArrayList<>();
        int r = 2;
        for (TFDAwareNeed need : TFDAwareNeeds) {

            List<Pair> list = new ArrayList<>();

            Row row = sheet.getRow(r);

            int c = 2;
            for (TFDAwareNeed other : TFDAwareNeeds) {

                final double similarity = querySimilarity.score(need, other);
                list.add(new Pair(other, similarity));
                row.createCell(c, CellType.NUMERIC).setCellValue(similarity);

                c++;
            }

            r++;

            Collections.sort(list);
            if (list.get(0).infoNeed.id() != need.id()) {
                mostSimilarIsNotItself.add(need);
                mostSimilarQueryShouldBeItSelf++;
            }
        }

        if (mostSimilarQueryShouldBeItSelf > 0) {
            System.out.println("Most similar query should be itself " + mostSimilarQueryShouldBeItSelf + " " + type + " " + querySimilarity.toString());
            System.out.println(mostSimilarIsNotItself);
        }

    }

    private void addColors(Sheet sheet, java.util.Collection<TFDAwareNeed> TFDAwareNeeds) {

        int i = 0;
        for (TFDAwareNeed need : TFDAwareNeeds) {
            Row row = sheet.createRow(i++);
            int[] colors = colorMap.get(winner(need));
            for (int c = 0; c < colors.length; c++)
                row.createCell(c, CellType.NUMERIC).setCellValue(colors[c]);
        }
    }

    private void addTopicHeaders(Sheet sheet, java.util.Collection<TFDAwareNeed> TFDAwareNeeds) {
        Row r0 = sheet.createRow(0);

        int c = 2;
        for (TFDAwareNeed need : TFDAwareNeeds) {
            r0.createCell(c, CellType.STRING).setCellValue(winner(need));
            c++;
        }

        Row r1 = sheet.createRow(1);

        c = 2;
        for (TFDAwareNeed need : TFDAwareNeeds) {
            r1.createCell(c, CellType.STRING).setCellValue(need.getDistinctQuery());
            c++;
        }

        int r = 2;
        for (TFDAwareNeed need : TFDAwareNeeds) {
            Row row = sheet.createRow(r);
            row.createCell(0, CellType.STRING).setCellValue(winner(need));
            r++;
        }

        r = 2;
        for (TFDAwareNeed need : TFDAwareNeeds) {
            Row row = sheet.getRow(r);
            row.createCell(1, CellType.STRING).setCellValue(need.getDistinctQuery());
            r++;
        }

    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }
}
