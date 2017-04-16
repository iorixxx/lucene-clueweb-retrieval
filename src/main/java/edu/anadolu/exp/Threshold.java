package edu.anadolu.exp;

import edu.anadolu.QueryBank;
import edu.anadolu.QuerySelector;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.NeedDouble;
import edu.anadolu.knn.Measure;
import edu.anadolu.stats.QueryStats;
import org.apache.commons.math3.util.Precision;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to draw discriminative ratios and model effectiveness graphs
 */
public class Threshold {

    private final QuerySelector selector;
    private final Path collectionPath;
    private final String tag;
    private final DataSet dataSet;
    private final Map<InfoNeed, QueryStats> queryStatsMap;


    public Threshold(Path collectionPath, DataSet dataSet, String tag) {
        this.collectionPath = collectionPath;
        this.dataSet = dataSet;
        this.tag = tag;
        this.selector = new QuerySelector(dataSet, tag);
        this.queryStatsMap = selector.loadQueryStatsMap();
    }

    static String id(List<NeedDouble> list) {

        StringBuilder builder = new StringBuilder();

        builder.append("ID").append("\t");

        for (NeedDouble needDouble : list) {
            builder.append(needDouble.need.id()).append("\t");
        }

        return builder.toString().trim();
    }

    static String ratio(List<NeedDouble> list) {

        StringBuilder builder = new StringBuilder();

        builder.append("Ratio").append("\t");

        for (NeedDouble needDouble : list) {
            builder.append(String.format("%.2f", needDouble.phraseness)).append("\t");
        }

        return builder.toString().trim();
    }

    String ruleBased(Evaluator evaluator, List<NeedDouble> list) throws IOException {

        StringBuilder builder = new StringBuilder();

        builder.append("RB").append("\t");

        for (NeedDouble needDouble : list) {
            InfoNeed need = needDouble.need;
            double score;
            if (need.wordCount() == 1)
                score = evaluator.score(need, "LogTFNv0L0");
            else if (selector.maxLambda(need) > 1.0)
                score = evaluator.score(need, "DFIC");
            else if (need.wordCount() == 2)
                if (selector.termRatio(need, "df") < 2)
                    score = evaluator.score(need, "LogTFNv0L0");
                else
                    score = evaluator.score(need, "DPH");
            else
                score = evaluator.score(need, "DFIC");

            builder.append(score).append("\t");
        }

        return builder.toString().trim();
    }

    static String entries(String model, Evaluator evaluator, List<NeedDouble> list) {

        StringBuilder builder = new StringBuilder();

        builder.append(model).append("\t");

        for (NeedDouble needDouble : list) {
            builder.append(evaluator.score(needDouble.need, model)).append("\t");
        }

        return builder.toString().trim();
    }


    /**
     * One term queries, double value is lamda of the term
     *
     * @param models term-weighting models to be depicted
     * @throws IOException
     */
    public void one(String models) throws IOException {

        QueryBank bank = new QueryBank(dataSet);
        List<InfoNeed> needs = bank.getOneTermQueries(0);

        Evaluator evaluator = new Evaluator(dataSet, tag, Measure.ERR20, models, "evals", "OR");


        List<NeedDouble> list = new ArrayList<>();

        for (InfoNeed need : needs)
            list.add(new NeedDouble(need, selector.termRatio(need, "meanvar")));

        Collections.sort(list);


        System.out.println(ratio(list));
        System.out.println(id(list));

        for (String model : models.split("_")) {
            System.out.println(entries(model, evaluator, list));
        }
    }

    /**
     * Queries that have a term having discrimination value larger than a threshold around 1.
     * double is maximum value of lambda of query terms
     *
     * @param models term-weighting models to be depicted
     * @throws IOException
     */
    public void lamda(String models) throws IOException {


        List<InfoNeed> needs = selector.lambdaQueries(1.0);

        Evaluator evaluator = new Evaluator(dataSet, tag, Measure.ERR20, models, "evals", "OR");


        List<NeedDouble> list = new ArrayList<>();

        for (InfoNeed need : needs)
            list.add(new NeedDouble(need, selector.maxLambda(need)));

        Collections.sort(list);

        System.out.println(ratio(list));
        System.out.println(id(list));

        for (String model : models.split("_")) {
            System.out.println(entries(model, evaluator, list));
        }
    }

    /**
     * Two term queries, double value is df ratio
     *
     * @param models term-weighting models to be depicted
     * @throws IOException
     */

    public void two(String models) throws IOException {

        QueryBank bank = new QueryBank(dataSet);
        List<InfoNeed> needs = bank.getTwoTermQueries(0);

        Evaluator evaluator = new Evaluator(dataSet, tag, Measure.ERR20, models, "evals", "OR");

        List<NeedDouble> list = new ArrayList<>();

        for (InfoNeed need : needs) {
            double d = queryStatsMap.get(need).meanDividedByVariance();
            list.add(new NeedDouble(need, d));
        }

        Collections.sort(list);

        System.out.println(ratio(list));
        System.out.println(id(list));
        for (String model : models.split("_")) {
            System.out.println(entries(model, evaluator, list));
        }
    }

    /**
     * All queries, double value is the query length
     *
     * @param models term-weighting models to be depicted
     * @throws IOException
     */

    public void all(String models) throws IOException {

        QueryBank bank = new QueryBank(dataSet);
        List<InfoNeed> needs = bank.getAllQueries(0);

        Evaluator evaluator = new Evaluator(dataSet, tag, Measure.ERR20, models, "evals", "OR");

        List<NeedDouble> list = new ArrayList<>();

        for (InfoNeed need : needs)
            list.add(new NeedDouble(need, need.wordCount()));

        Collections.sort(list);


        System.out.println(ratio(list));
        System.out.println(id(list));
        for (String model : models.split("_")) {
            System.out.println(entries(model, evaluator, list));
        }

        System.out.println(ruleBased(evaluator, list));
    }

    /**
     * Three four term queries, double value is term discrimination ratio
     *
     * @param models term-weighting models to be depicted
     * @throws IOException
     */

    public void remaining(String models, String ratio) throws IOException {

        List<InfoNeed> needs = selector.remaining(1.0);

        Evaluator evaluator = new Evaluator(dataSet, tag, Measure.ERR20, models, "evals", "OR");

        List<NeedDouble> list = new ArrayList<>();

        for (InfoNeed need : needs)
            list.add(new NeedDouble(need, selector.termRatio(need, ratio)));

        Collections.sort(list);


        System.out.println(ratio(list));
        System.out.println(id(list));
        for (String model : models.split("_")) {
            System.out.println(entries(model, evaluator, list));
        }
    }

    public void centik() throws IOException {

        List<Path> list = Files.walk(Paths.get(collectionPath.toString(), "verbose_freqs", tag), 1)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        for (Path p : list) {

            if (Files.isDirectory(p)) continue;
            if (Files.isHidden(p)) continue;

            int max = process(p);

            String term = p.getFileName().toString();

            long TF = selector.termStatisticsMap.get(term).totalTermFreq();

            long df = selector.termStatisticsMap.get(term).docFreq();

            long c = selector.numberOfDocuments;

            if (max > 1) {
                System.out.println(max + " " + Precision.round((double) TF / df, 4) + " " + Precision.round((double) TF / c, 4) + " " + p.getFileName());
            }
        }
    }

    private int process(Path p) throws IOException {

        List<String> lines = Files.readAllLines(p, StandardCharsets.US_ASCII);

        List<Long> counts = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("value")) continue;


            try {
                String[] parts = line.split("\t");
                long count = Long.parseLong(parts[1]);
                counts.add(count);

            } catch (java.lang.NumberFormatException nfe) {
                System.out.println(p.toString());
            }

        }

        long max = Collections.max(counts);

        for (int i = 0; i < counts.size(); i++)
            if (counts.get(i) == max) return i + 1;

        return -1;

    }

    public static void main(String[] args) throws IOException {

        final String tfd_home = "/Users/iorixxx/clueweb09";

        Collection collection = Collection.CW09A;

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        Path collectionPath = Paths.get(tfd_home, collection.toString());

        Threshold threshold = new Threshold(collectionPath, dataSet, "KStemAnalyzer");

        threshold.one("DPH_RawTF");
        threshold.two("DFIC_DPH_LogTFNv0L0");
        threshold.lamda("DPH_DFIC_LGDc16.25");
        threshold.all("LogTFNv0L0_DPH_LGDc16.25_DFIC");

        threshold.selector.nonDecreasingQueries(2);

        threshold.centik();

        QueryBank bank = new QueryBank(dataSet);

        threshold.remaining("LogTFNv0L0_DPH_DFIC", "df");

        System.out.println("====== two term queries sorted by docCount");
        threshold.two("DFIC_DPH_LogTFNv0L0");

        System.out.println(Long.MAX_VALUE);

        for (double k = 0.2; k <= 3.1; k += 0.2)
            System.out.print(String.format("%.1f", k) + ", ");
    }
}
