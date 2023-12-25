package edu.anadolu.cmdline;

import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.*;
import edu.anadolu.spam.SubmissionFile;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.analysis.Analyzer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature Extraction Tool
 */
public final class FeatureTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;
    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();
    @Option(name = "-measure", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;
    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;
    @Option(name = "-out", required = true, usage = "output file")
    private String out;

    public static final String DEFAULT_MODELS = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0_DPH_DFIC_DFRee_DLH13";

    @Override
    public String getShortDescription() {
        return "Feature Extraction Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    protected String evalDirectory(DataSet dataset) {
        if (Collection.GOV2.equals(dataset.collection()) || Collection.MC.equals(dataset.collection()) || Collection.ROB04.equals(dataset.collection())) {
            return "evals";
        } else {
            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, "OR");
            return bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }
    }

    Map<Integer, double[]> resultListFeatures(DataSet dataset, String tag) throws IOException {

        // final Path runs_path = dataset.collectionPath().resolve("runs").resolve(tag).resolve(track.toString());
        //
        //    List<Path> fileList = Files.walk(runs_path)
        //           .filter(Files::isRegularFile)
        //          .collect(Collectors.toList());

        String[] models = DEFAULT_MODELS.split("_");

        Map<String, Map<Integer, List<SubmissionFile.Tuple>>> theMap = new HashMap<>();

        for (String model : models) {

            final Map<Integer, List<SubmissionFile.Tuple>> entries = new LinkedHashMap<>();

            int counter = 0;

            for (Track track : dataset.tracks()) {

                Path thePath = Paths.get(dataset.collectionPath().toString(), "runs", tag, track.toString(), model + "_contents_" + tag + "_" + "OR_all.txt");

                if (!Files.exists(thePath) || !Files.isRegularFile(thePath) || !Files.isReadable(thePath))
                    throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");


                final SubmissionFile submissionFile = new SubmissionFile(thePath);
                counter += submissionFile.size();
                entries.putAll(submissionFile.entryMap());

            }

            if (counter != entries.size()) throw new RuntimeException("map sizes are not equal!");

            theMap.put(model, entries);

        }

        Map<Integer, double[]> features = new HashMap<>();

        for (InfoNeed need : dataset.getTopics()) {

            final List<SubmissionFile.Tuple> reference = theMap.get(models[0]).get(need.id());

            double[] values = new double[models.length - 1];

            for (int i = 1; i < models.length; i++) {
                List<SubmissionFile.Tuple> alternate = theMap.get(models[i]).get(need.id());
                values[i - 1] = systemSimilarity(reference, alternate);
            }
            printFeatures(Integer.toString(need.id()), values);
            features.put(need.id(), values);
        }
        return features;
    }

    /**
     * The ratio of Intersection over Union. The Jaccard coefficient measures similarity between finite sample sets,
     * and is defined as the size of the intersection divided by the size of the union of the sample sets.
     *
     * @param reference set1
     * @param alternate set1
     * @return Note that by design,  0 <= J(A,B) <=1. If A and B are both empty, define J(A,B) = 1.
     */
    private static double systemSimilarity(List<SubmissionFile.Tuple> reference, List<SubmissionFile.Tuple> alternate) {

        Set<String> set1 = reference.stream().map(SubmissionFile.Tuple::docID).collect(Collectors.toSet());
        Set<String> set2 = alternate.stream().map(SubmissionFile.Tuple::docID).collect(Collectors.toSet());

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2); // Union

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2); // Intersection

        return (double) intersection.size() / union.size();
    }

    @Override
    public void run(Properties props) throws Exception {

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        if ("labels".equals(task)) {
            Evaluator evaluator = new Evaluator(dataset, tag, measure, DEFAULT_MODELS, "evals"/*evalDirectory(dataset)*/, "OR");
            List<InfoNeed> needs = evaluator.getNeeds();

            // Print header
            System.out.println("QueryID\tWinner\t" + measure + "\tLoser\t" + measure);
            for (InfoNeed need : needs) {
                System.out.println("qid:" + need.id() + "\t" + evaluator.bestModel(need, false) + "\t" + evaluator.bestModelScore(need, false) + "\t" + evaluator.bestModel(need, true) + "\t" + evaluator.bestModelScore(need, true));
            }
            return;
        } else if ("list".equals(task)) {
            resultListFeatures(dataset, this.tag);
            return;
        } else if ("export".equals(task)) {
            export(dataset);
            return;
        }

        Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));

        PMI pmi = new PMI(dataset.indexesPath().resolve(tag), "contents");
        SCS scs = new SCS(dataset.indexesPath().resolve(tag), "contents");
        SCQ scq = new SCQ(dataset.indexesPath().resolve(tag));
        IDF idf = new IDF(dataset.indexesPath().resolve(tag));
        CTI cti = new CTI(dataset.indexesPath().resolve(tag));
        Scope scope = new Scope(dataset.indexesPath().resolve(tag));

        QuerySelector querySelector = new QuerySelector(dataset, tag);
        boolean term = "term".equals(task);

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(out), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            // Print header
            System.out.println("QueryID\tWordCount\tGamma\tOmega\tAvgPMI\tSCS\tMeanIDF\tVarIDF\tMeanCTI\tVarCTI\tMeanSkew\tVarSkew\tMeanKurt\tVarKurt\tMeanSCQ\tVarSCQ");
            for (InfoNeed need : querySelector.allQueries) {

                Map<String, String> map = querySelector.getFrequencyDistributionList(need, "contents_all_freq_1000.csv");

                List<String> analyzedTokens = Analyzers.getAnalyzedTokens(need.query(), analyzer);

                double[] idfs = new double[analyzedTokens.size()];
                double[] ctis = new double[analyzedTokens.size()];
                double[] skew = new double[analyzedTokens.size()];
                double[] kurt = new double[analyzedTokens.size()];
                double[] scqs = new double[analyzedTokens.size()];

                for (int c = 0; c < analyzedTokens.size(); c++) {
                    String word = analyzedTokens.get(c);
                    String freqLine = map.get(word);
                    DescriptiveStatistics descriptiveStatistics = querySelector.toDescriptiveStatistics(freqLine);

                    idfs[c] = idf.value(word);
                    ctis[c] = cti.value(word);
                    skew[c] = descriptiveStatistics.getSkewness();
                    kurt[c] = descriptiveStatistics.getKurtosis();
                    scqs[c] = scq.value(word);
                    if (term)
                        System.out.println(need.id() + ":" + word + "\t" + idfs[c] + "\t" + ctis[c] + "\t" + skew[c] + "\t" + kurt[c]);
                }

                System.out.print("qid:" + need.id() + "\t" + need.wordCount() + "\t" + idf.aggregated(need, new Aggregate.Gamma1()) + "\t" + scope.value(need) + "\t");
                System.out.print(pmi.value(need) + "\t" + scs.value(need) + "\t");
                System.out.print(StatUtils.mean(idfs) + "\t" + StatUtils.variance(idfs) + "\t");
                System.out.print(StatUtils.mean(ctis) + "\t" + StatUtils.variance(ctis) + "\t");
                System.out.print(StatUtils.mean(skew) + "\t" + StatUtils.variance(skew) + "\t" + StatUtils.mean(kurt) + "\t" + StatUtils.variance(kurt) + "\t");
                System.out.println(StatUtils.mean(scqs) + "\t" + StatUtils.variance(scqs));

                String line = "qid:" + need.id() +
                        "\tWordCount:" + need.wordCount() +
                        "\tGamma:" + String.format("%.5f",idf.aggregated(need, new Aggregate.Gamma1())) +
                        "\tOmega:" +  String.format("%.5f",scope.value(need)) +
                        "\tAvgPMI:" + String.format("%.5f",pmi.value(need)) +
                        "\tSCS:" + String.format("%.5f",scs.value(need)) +
                        "\tMeanIDF:" + String.format("%.5f",StatUtils.mean(idfs)) +
                        "\tVarIDF:" + String.format("%.5f",StatUtils.variance(idfs)) +
                        "\tMeanCTI:" +  String.format("%.5f",StatUtils.mean(ctis)) +
                        "\tVarCTI:" + String.format("%.5f",StatUtils.variance(ctis)) +
                        "\tMeanSkew:" + String.format("%.5f",StatUtils.mean(skew)) +
                        "\tVarSkew:" + String.format("%.5f",StatUtils.variance(skew)) +
                        "\tMeanKurt:" + String.format("%.5f",StatUtils.mean(kurt)) +
                        "\tVarKurt:" + String.format("%.5f",StatUtils.variance(kurt)) +
                        "\tMeanSCQ:" + String.format("%.5f",StatUtils.mean(scqs)) +
                        "\tVarSCQ:" + String.format("%.5f",StatUtils.variance(scqs));
                System.out.println(line);
                bw.write(line);
                bw.newLine();
            }


        }catch (Exception ex){
            ex.printStackTrace();
            printFeatures("qid:" + need.id() + "\t" + need.wordCount(),
                    idf.aggregated(need, new Aggregate.Gamma1()),
                    scope.value(need),
                    pmi.value(need),
                    scs.value(need),
                    StatUtils.mean(idfs), StatUtils.variance(idfs),
                    StatUtils.mean(ctis), StatUtils.variance(ctis),
                    StatUtils.mean(skew), StatUtils.variance(skew),
                    StatUtils.mean(kurt), StatUtils.variance(kurt),
                    StatUtils.mean(scqs), StatUtils.variance(scqs)
            );
        }

        pmi.close();
        scs.close();
        scq.close();
        idf.close();
        cti.close();
        scope.close();
    }

    static void printFeatures(String qid, double... values) {
        System.out.print(qid);
        for (double d : values) {
            System.out.printf("\t%.5f", d);
        }
        System.out.println();
    }

    private void export(DataSet dataset) throws Exception {

        Path excelPath = Paths.get(tfd_home, collection.toString()).resolve("excels");
        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        Path excelFile = excelPath.resolve(collection + "Features.xlsx");

        Workbook workbook = new XSSFWorkbook();

        for (final Path path : discoverIndexes(dataset)) {

            final String tag = path.getFileName().toString();

            Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));

            Map<Integer, double[]> features = resultListFeatures(dataset, tag);

            PMI pmi = new PMI(path, "contents");
            SCS scs = new SCS(path, "contents");
            SCQ scq = new SCQ(path);
            IDF idf = new IDF(path);
            CTI cti = new CTI(path);
            Scope scope = new Scope(path);

            QuerySelector querySelector = new QuerySelector(dataset, tag);

            Sheet statSheet = workbook.createSheet(tag);

            Row r0 = statSheet.createRow(0);

            String header = "QueryID\tWordCount\tGamma\tOmega\tAvgPMI\tSCS\tMeanIDF\tVarIDF\tMeanCTI\tVarCTI\tMeanSkew\tVarSkew\tMeanKurt\tVarKurt\tMeanSCQ\tVarSCQ";
            header += "\tDLM\tLGD\tPL2\tDPH\tDFIC\tDFRee\tDLH13";

            int i = 0;
            for (String name : header.split("\t")) {
                Cell cell = r0.createCell(i++);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(name);
            }

            int row = 1;
            for (InfoNeed need : querySelector.allQueries) {

                Map<String, String> map = querySelector.getFrequencyDistributionList(need, "contents_all_freq_1000.csv");
                List<String> analyzedTokens = Analyzers.getAnalyzedTokens(need.query(), analyzer);

                double[] idfs = new double[analyzedTokens.size()];
                double[] ctis = new double[analyzedTokens.size()];
                double[] skew = new double[analyzedTokens.size()];
                double[] kurt = new double[analyzedTokens.size()];
                double[] scqs = new double[analyzedTokens.size()];

                for (int c = 0; c < analyzedTokens.size(); c++) {
                    String word = analyzedTokens.get(c);
                    String freqLine = map.get(word);
                    DescriptiveStatistics descriptiveStatistics = querySelector.toDescriptiveStatistics(freqLine);

                    idfs[c] = idf.value(word);
                    ctis[c] = cti.value(word);
                    skew[c] = descriptiveStatistics.getSkewness();
                    kurt[c] = descriptiveStatistics.getKurtosis();
                    scqs[c] = scq.value(word);

                }

                Row r = statSheet.createRow(row);
                Cell cell = r.createCell(0);
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(need.id());

                cell = r.createCell(1);
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(need.wordCount());


                i = insertCells(r, 2,
                        idf.aggregated(need, new Aggregate.Gamma1()),
                        scope.value(need),
                        pmi.value(need),
                        scs.value(need),
                        StatUtils.mean(idfs), StatUtils.variance(idfs),
                        StatUtils.mean(ctis), StatUtils.variance(ctis),
                        StatUtils.mean(skew), StatUtils.variance(skew),
                        StatUtils.mean(kurt), StatUtils.variance(kurt),
                        StatUtils.mean(scqs), StatUtils.variance(scqs));

                insertCells(r, i, features.get(need.id()));

                row++;
            }

            pmi.close();
            scs.close();
            scq.close();
            idf.close();
            cti.close();
            scope.close();
        }

        workbook.write(Files.newOutputStream(excelFile));
        workbook.close();
        System.out.println(collection + "'s query feature matrix is saved into : " + excelFile.toAbsolutePath());
    }

    private static int insertCells(Row r, int i, double... values) {

        for (double d : values) {
            Cell cell = r.createCell(i++);
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(d);
        }
        return i;
    }
}
