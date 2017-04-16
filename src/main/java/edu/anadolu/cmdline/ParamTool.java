package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.BM25c;
import edu.anadolu.similarities.DirichletLM;
import edu.anadolu.similarities.LGDc;
import edu.anadolu.similarities.PL2c;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.search.similarities.ModelBase;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Free-parameter tuning tool
 */
public final class ParamTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Determining Free-Parameter Values";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "LGDc*";


    @Option(name = "-tag", metaVar = "[KStemAnalyzer|KStemAnalyzerAnchor]", required = false, usage = "Index Tag")
    private String tag = "KStemAnalyzer";

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    Measure measure = Measure.NDCG100;

    @Option(name = "-top", metaVar = "[-1|10]", required = false, usage = "maximum number of model-score paris to display")
    private int top = 10;

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    String op = "OR";

    public static final DecimalFormat df;

    static {
        df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
    }

    static String cacheKey(String model, String tag, Measure measure, String op) {
        return model + "_" + tag + "_" + measure.metric().toString() + "_" + measure.k() + "_" + op;
    }


    private static KB handleKB(String model) {

        int b = model.indexOf("b");
        if (b == -1) throw new RuntimeException("Cannot find b letter in the model name : " + model);
        double bValue = Double.parseDouble(model.substring(b + 1));

        int k = model.indexOf("k");
        if (k == -1) throw new RuntimeException("Cannot find k letter in the model name : " + model);

        double kValue = Double.parseDouble(model.substring(k + 1, b));

        return new KB(kValue, bValue);
    }

    static ModelBase string2model(String model) {
        if (model.contains("k") && model.contains("b")) {
            KB kb = handleKB(model);
            return new BM25c(kb.k, kb.b);
        } else if (model.contains("c")) {
            double c = Double.parseDouble(extractCString(model));

            if (model.startsWith("LGD"))
                return new LGDc(c);

            if (model.startsWith("PL2"))
                return new PL2c(c);

            if (model.startsWith("DirichletLM"))
                return new DirichletLM(c);
        }

        throw new RuntimeException("unexpected mode : " + model);
    }

    /**
     * Training of free-parameters
     *
     * @param model BM25, PL2, or LGD
     * @return LGDc18.5, DirichletLMc1500.0, BM25k3.0b0.1,  etc.
     */
    public static ModelBase train(String model, DataSet dataSet, String tag, Measure measure, String op) {

        String key = cacheKey(model, tag, measure, op);

        Properties properties = cacheProperties(dataSet);

        if (properties.getProperty(key) != null) {
            String value = properties.getProperty(key);
            return string2model(value);
        }

        Evaluator evaluator = new Evaluator(dataSet, tag, measure, model + "*", "parameter_evals", op);

        String bestModel = evaluator.bestModel().model;

        properties.put(key, bestModel);
        saveCacheProperties(dataSet, properties);

        return string2model(bestModel);
    }

    public static ModelBase train(String model, DataSet[] dataSets, String tag, Measure measure, String op) {

        String[] evalDirs = new String[dataSets.length];
        StringBuilder key = new StringBuilder("");

        for (int i = 0; i < dataSets.length; i++) {
            evalDirs[i] = "parameter_evals";
            key.append(dataSets[i].collection().toString()).append("_");
        }

        key.append(cacheKey(model, tag, measure, op));

        Path cacheFile = dataSets[0].home().resolve("cache.properties");

        Properties properties = cacheProperties(cacheFile);

        if (properties.getProperty(key.toString()) != null) {
            String value = properties.getProperty(key.toString());
            return string2model(value);
        }

        Evaluator evaluator = new Evaluator(dataSets, tag, measure, model + "*", evalDirs, op);
        String bestModel = evaluator.bestModel().model;
        properties.put(key.toString(), bestModel);

        try (OutputStream out = Files.newOutputStream(cacheFile)) {
            properties.store(out, null);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return string2model(bestModel);
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

        if (dataset == null) {
            System.out.println(collection + " returned null dataset");
            return;
        }

        Evaluator evaluator = new Evaluator(dataset, tag, measure, models, "parameter_evals", op);

        System.out.println("========= mean effective measures ===========");
        evaluator.printMeanWT(top);
        evaluator.printMean(top);

        double[] values = new double[dataset.tracks().length * dataset.tracks().length - dataset.tracks().length];
        Arrays.fill(values, 0.0);
        int c = 0;

        List<String> bestModelList = new ArrayList<>();

        System.out.println("========= best model per track ===========");
        for (Track track : dataset.tracks()) {
            ModelScore bestModelScore = evaluator.bestModelPerTrack(track);
            bestModelList.add(bestModelScore.model);
            System.out.println(track + " : " + bestModelScore);

            for (Track rest : dataset.tracks()) {
                if (rest.equals(track)) continue;
                ModelScore modelScorePerTrack = evaluator.modelScorePerTrack(rest, bestModelScore.model);
                System.out.println("\t" + rest + " : " + modelScorePerTrack);
                values[c++] = modelScorePerTrack.score;
            }
        }

        System.out.println("========= overall jack knife result is : " + StatUtils.mean(values));
        System.out.println("========= track aware best : " + modelFromBestAverageFreeParameters(bestModelList));
        System.out.println("========== collection best : " + evaluator.bestModel());

    }

    private static class KB {
        final double k;
        final double b;

        KB(double k, double b) {
            this.k = k;
            this.b = b;
        }

        public String toString() {
            return "k=" + k + ",b=" + b;
        }
    }

    private static KB handleKB(List<String> bestModelList) {

        BigDecimal bValuesSum = new BigDecimal("0.0");
        BigDecimal kValuesSum = new BigDecimal("0.0");

        for (String model : bestModelList) {
            int b = model.indexOf("b");
            if (b == -1) throw new RuntimeException("Cannot find b letter in the model name : " + model);
            bValuesSum = bValuesSum.add(new BigDecimal(model.substring(b + 1)));

            int k = model.indexOf("k");
            if (k == -1) throw new RuntimeException("Cannot find k letter in the model name : " + model);

            kValuesSum = kValuesSum.add(new BigDecimal(model.substring(k + 1, b)));

        }

        int size = bestModelList.size();

        return new KB(divide(kValuesSum, size), divide(bValuesSum, size));
    }

    private static double divide(BigDecimal sum, int size) {
        try {
            return sum.divide(new BigDecimal(size)).doubleValue();
        } catch (java.lang.ArithmeticException a) {
            String c = df.format(sum.doubleValue() / size);
            // System.out.println(sum.doubleValue() / size + " " + c + " " + Double.parseDouble(c));
            return Double.parseDouble(c);
        }
    }

    private static double handleC(List<String> bestModelList) {

        BigDecimal cValuesSum = new BigDecimal("0.0");

        for (String model : bestModelList) {
            cValuesSum = cValuesSum.add(extractCValue(model));
        }

        return divide(cValuesSum, bestModelList.size());
    }

    static ModelBase modelFromBestAverageFreeParameters(List<String> bestModelList) {

        String model = bestModelList.get(0);
        if (model.contains("k") && model.contains("b")) {
            KB kb = handleKB(bestModelList);
            return new BM25c(kb.k, kb.b);
        } else if (model.contains("c")) {
            double c = handleC(bestModelList);

            if (model.startsWith("LGD"))
                return new LGDc(c);

            if (model.startsWith("PL2"))
                return new PL2c(c);

            if (model.startsWith("DirichletLM"))
                return new DirichletLM(c);
        }

        throw new RuntimeException("unexpected model : " + model);
    }

    private static String extractCString(String model) {

        int i = model.lastIndexOf("c");

        if (i == -1) throw new RuntimeException("Cannot find c letter in the model name : " + model);

        return model.substring(i + 1);

    }

    static BigDecimal extractCValue(String model) {

        String c = extractCString(model);

        try {
            return new BigDecimal(c);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("here is the number: " + model, nfe);
        }
    }
}
