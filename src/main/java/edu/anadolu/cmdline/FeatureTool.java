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
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

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

    void resultListFeatures(DataSet dataset) throws IOException {

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

        for (InfoNeed need : dataset.getTopics()) {

            final List<SubmissionFile.Tuple> reference = theMap.get(models[0]).get(need.id());

            double[] values = new double[models.length - 1];

            for (int i = 1; i < models.length; i++) {
                List<SubmissionFile.Tuple> alternate = theMap.get(models[i]).get(need.id());
                values[i - 1] = systemSimilarity(reference, alternate);
            }
            printFeatures(Integer.toString(need.id()), values);
        }
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
            resultListFeatures(dataset);
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
}
