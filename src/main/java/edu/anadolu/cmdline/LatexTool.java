package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Solution;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.ModelBase;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.cmdline.ParamTool.train;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * LaTex Tool
 */
public final class LatexTool extends EvaluatorTool {

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Override
    public String getShortDescription() {
        return "LaTex Utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    DataSet dataset;

    public static String prettyDataSet(String dataSet) {

        if ("CW09ACW12B".equals(dataSet)) return "ClueWeb\\{09A\\textbar12B\\}";
        if ("MQ09".equals(dataSet)) return "Million Query 2009";
        if ("MQ08".equals(dataSet)) return "Million Query 2008";
        if ("MQ07".equals(dataSet)) return "Million Query 2007";
        if ("MQE1".equals(dataSet)) return "Million Query 2009 Extended1";
        if ("MQE2".equals(dataSet)) return "Million Query 2009 Extended2";
        if ("CW09A".equals(dataSet)) return "ClueWeb09A";
        if ("CW09B".equals(dataSet)) return "ClueWeb09B";
        if ("CW12B".equals(dataSet)) return "ClueWeb12-B13";
        if ("CW12A".equals(dataSet)) return "ClueWeb12A";
        if ("ROB04".equals(dataSet)) return "Robust Track 2004";
        if ("GOV2".equals(dataSet)) return "Terabyte Track";
        throw new RuntimeException(dataSet + " is unrecognized!");
    }

    public static String latexModel(String model) {
        return latexModel(model, true);
    }

    public static String latexModel(String model, boolean printFreeParameters) {

        if ("LGDL2".equals(model)) return printFreeParameters ? "LGD (" + handleParam(model) + ")" : "LGD";

        if ("LogTFNv0L0".equals(model)) return "LogTF";

        if (model.startsWith("HYBRID")) return "RB";

        if (model.startsWith("LGDc")) return printFreeParameters ? "LGD (" + handleParam(model) + ")" : "LGD";

        if (model.startsWith("BM25k")) return printFreeParameters ? "BM25 (" + handleParam(model) + ")" : "BM25";

        if (model.startsWith("PL2c")) return printFreeParameters ? "PL2 (" + handleParam(model) + ")" : "PL2";

        if (model.startsWith("DirichletLMc"))
            return printFreeParameters ? "Dirichlet (" + handleParam(model) + ")" : "DLM";

        if ("DIV_Cm".equals(model)) return "SEL";

        if ("RND".equals(model)) return "\\bfseries RND";

        if ("MLE".equals(model)) return "\\bfseries MLE";

        return model;
    }

    public static String handleParam(String model) {
        if (model.startsWith("BM25")) {
            int b = model.indexOf("b");
            if (b == -1) throw new RuntimeException("Cannot find b letter in the model name : " + model);

            String bValue = model.substring(b + 1);


            int k = model.indexOf("k");
            if (k == -1) throw new RuntimeException("Cannot find k letter in the model name : " + model);

            String kValue = model.substring(k + 1, b);

            return "$k_1$=" + kValue + " $b$=" + bValue;
        }

        if (model.startsWith("PL2") || model.startsWith("LGD")) {

            BigDecimal c = ParamTool.extractCValue(model);

            return "$c$=" + c.toString();
        }

        if (model.startsWith("DirichletLM")) {

            BigDecimal c = ParamTool.extractCValue(model);

            String s = c.toString();
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);

            return "$\\mu$=" + s;
        }

        throw new RuntimeException("unknown model : " + model);
    }

    List<Solution> bringKNNResult(Path collectionPath, String tag) {

        Path path = Paths.get(collectionPath.toString(), "results", tag, measure + op + ".txt");

        if (!Files.isRegularFile(path) || !Files.exists(path) || !Files.isReadable(path))
            throw new IllegalArgumentException(path + " does not exist or is not a file");


        final List<Solution> solutionList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.US_ASCII)) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;

                if (!line.startsWith("***")) continue;


                if (line.startsWith("***")) line = line.substring(3);
                String[] parts = whiteSpaceSplitter.split(line);

                Solution solution = Solution.parseSolution(parts);
                solutionList.add(solution);

            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Collections.sort(solutionList);


        return Collections.unmodifiableList(solutionList);
    }

    ModelScore find(String voter, String similarity, String chi, List<Solution> solutionList) {
        Solution solution = findSolution(voter, similarity, chi, solutionList);
        return new ModelScore("Selective (" + similarity + ")(k=" + solution.k + ")", solution.getMean());
    }

    Solution findSolution(String voter, String similarity, String chi, List<Solution> solutionList) {

        for (Solution solution : solutionList) {

            if (solution.voter.startsWith(voter) && solution.getRunLabel().startsWith(similarity + (chi == null ? "" : "_" + chi)))
                return solution;
        }
        throw new RuntimeException("cannot find the solution : " + voter + "_" + similarity + "_" + chi);
    }

    /**
     * Return different k values
     *
     * @param voter        MeanVoter
     * @param runLabel     Average_UnEqualDataPointsl=0u=400divide=truecdf=false
     * @param solutionList list belong to the metric
     * @return list of different k values
     */
    List<Solution> findSolutionList(String voter, String runLabel, List<Solution> solutionList) {

        final List<Solution> inner = new ArrayList<>();
        for (Solution solution : solutionList) {
            if (solution.voter.startsWith(voter) && solution.getRunLabel().equals(runLabel))
                inner.add(solution);
        }
        return inner;
    }

    String inner(String tag) throws IOException, ParseException {

        Evaluator evaluator = new Evaluator(dataset, tag, measure, models, "evals", op);


        final Set<InfoNeed> allZero = evaluator.getAllZero();
        final Set<InfoNeed> allSame = evaluator.getAllSame();


        evaluator = evaluator.evaluatorFromResidualNeeds();

        List<ModelScore> list = evaluator.averageForAllModels();


        list.add(evaluator.oracleMax());
        list.add(evaluator.oracleMin());
        list.add(evaluator.random());
        list.add(evaluator.randomX());

        //   ModelSelection modelSelection = new ModelSelection(dataset, tag);
        //    list.add(modelSelection.evaluate(evaluator));


//        final List<Solution> solutionList = bringKNNResult(dataset.collectionPath(), tag);
//
//        list.add(find("MeanVoter", "MetaTerm", "UnEqualDataPoints", solutionList));
//        //list.add(find("MajorityVoter", "MetaTerm", "UnEqualDataPoints", solutionList));
//
//        list.add(find("MeanVoter", "Average", "UnEqualDataPoints", solutionList));
//        //list.add(find("MajorityVoter", "Average", "UnEqualDataPoints", solutionList));


        Collections.sort(list);
        System.out.println("%========= " + measure + " results  ===========");

        StringBuilder builder = new StringBuilder();

        for (ModelScore modelScore : list) {

            String model = modelScore.model;
            model = model.replace("±", "$\\pm$");

            if (model.startsWith("BM25")) {
                builder.append("BM25");
            } else if (model.startsWith("PL2")) {
                builder.append("PL2");
            } else if (model.startsWith("LGD")) {
                builder.append("LGD");
            } else if (model.startsWith("DirichletLM")) {
                builder.append("Dirichlet");
            } else if ("globalOracle".equals(model))
                builder.append("Oracle");
            else
                builder.append(model);

            builder.append(" & ").append(String.format("%.5f ", modelScore.score)).append("\\\\");
            builder.append(System.lineSeparator());
        }

        builder.append("\\hline");
        builder.append(System.lineSeparator());

        builder.append(String.format("\\multicolumn{2}{|c|}{\\texttt{same=%d, zero=%d}} \\\\", allSame.size(), allZero.size()));
        return builder.toString();
    }


    final String TABLE = "\\begin{table}\n" +
            "\\caption{ClueWeb09B results in terms of %s@%d retrieval effectiveness}\n" +
            "\\label{tbl:res:%s%d}\n" +
            "\\centering\n" +
            "\\begin{tabular}{| c | c | c |}\n" +
            "\\hline \n" +
            "\\multicolumn{2}{|c|}{\\texttt{KStem}} \\\\\n" +
            "\\hline\n" +
            "\\bfseries Model  &  %s  \\\\\n" +
            "\\hline \n" +
            "\n" +
            "%s" +
            "\n" +
            "\\hline\n" +
            "\\end{tabular}\n" +
            "\\quad\n" +
            "\\begin{tabular}{| c | c | c |}\n" +
            "\\hline \n" +
            "\\multicolumn{2}{|c|}{\\texttt{KStemAnchor}} \\\\\n" +
            "\\hline\n" +
            "\\bfseries Model  &  %s  \\\\\n" +
            "\\hline \n" +
            "\n" +
            "%s" +
            "\n" +
            "\\hline\n" +
            "\\end{tabular}\n" +
            "\\end{table}\n";

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        dataset = CollectionFactory.dataset(collection, tfd_home);


        if ("spam".equals(task)) {
            spam(tfd_home);
            return;
        }

        if ("param".equals(task)) {
            param(tfd_home);
            return;
        }

        if ("acc".equals(task)) {
            acc(tfd_home);
            return;
        }

        if ("sens".equals(task)) {
            sensitive(tfd_home, "Average");
            return;
        }

        System.out.println(
                String.format(TABLE,
                        measure.metric().toString(),
                        measure.k(),
                        measure.metric().toString(),
                        measure.k(),
                        measure.metric().toString(),
                        inner("KStem"),
                        measure.metric().toString(),
                        inner("KStemAnchor")
                ));

    }

    private void spam(String home) {
        System.out.println("========= spam threshold  ===========");

        System.out.print(" & ");

        for (Collection collection : Arrays.asList(Collection.CW09A, Collection.CW09B, Collection.MQ09))
            for (String tag : tags)
                System.out.print(" & " + tag);

        System.out.print(" \\\\ ");
        System.out.println();
        System.out.println("\\hline");

        StringBuilder builder = new StringBuilder();

        for (Measure measure : Measure.values()) {

            builder.append(measure);


            for (Collection collection : Arrays.asList(Collection.CW09A, Collection.CW09B, Collection.MQ09))
                for (String tag : tags) {

                    int t = SpamEvalTool.bestSpamThreshold(CollectionFactory.dataset(collection, home), tag, measure, "OR");
                    builder.append(" & ").append(t);


                }

            builder.append(" \\\\ ");
            builder.append("\n");
            builder.append("\\hline");
            builder.append("\n");
        }

        System.out.println(builder.toString());
    }

    private void param(String home) {
        System.out.println("========= trained free-parameters  ===========");

        System.out.print(" & ");
        for (String parametricModel : parametricModels)
            System.out.print(" & " + parametricModel);

        System.out.print(" \\\\ ");
        System.out.println();
        System.out.println("\\hline");


        for (Measure measure : Measure.values()) {

            System.out.print(dataset.collection() + " & " + measure);


            for (String parametricModel : parametricModels) {

                ModelBase modelBase = train(parametricModel, dataset, tag, measure, op);
                System.out.print(" & ");

                System.out.print(handleParam(modelBase.toString()));

            }

            System.out.print(" \\\\ ");
            System.out.println();
            System.out.println("\\hline");
        }
    }

    private void acc(String home) {

        System.out.println("========= multi-label classification sigma1  ===========");

        System.out.print("\\bfseries");
        for (Measure measure : Measure.values()) {

            System.out.print(" &  \\bfseries ");
            System.out.print(measure.toString());

        }
        System.out.println(" \\\\");
        System.out.println("\\hline");

        for (String similarity : new String[]{"MetaTerm", "Average"}) {

            System.out.print(similarity);

            for (Measure measure : Measure.values()) {

                this.measure = measure;
                final List<Solution> solutionList = bringKNNResult(dataset.collectionPath(), tag);

                Solution solution = findSolution("MeanVoter", similarity, "UnEqualDataPoints", solutionList);

                System.out.print(" & ");

                System.out.print(solution.sigma1);

                //System.out.print("(" + solution.getMean() + ")");

            }

            System.out.print(" \\\\ ");
            System.out.println();
            System.out.println("\\hline");
        }
    }

    /**
     * Generates data for sensitivity curves
     *
     * @param home
     * @param similarity
     */

    private void sensitive(String home, String similarity) {

        System.out.println("========= parameter sensitivity  ===========");


        for (Measure measure : Measure.values()) {

            this.measure = measure;
            final List<Solution> solutionList = bringKNNResult(dataset.collectionPath(), tag);

            Solution solution = findSolution("MeanVoter", similarity, "UnEqualDataPoints", solutionList);

            double[] array = new double[100];
            Arrays.fill(array, 99.99);


            System.out.print(measure.toString());


            // System.out.print(solution.getRunLabel());

            List<Solution> inner = findSolutionList("MeanVoter", solution.getRunLabel(), solutionList);

            Collections.sort(inner, new Comparator<Solution>() {
                @Override
                public int compare(Solution o1, Solution o2) {
                    return o1.k - o2.k;
                }
            });

            for (Solution s : inner) {
                array[s.k] = s.getMean();
            }


            for (int i = 1; i < array.length; i++) {
                System.out.print("\t");
                System.out.print(array[i]);
            }

            System.out.println();

            System.out.print("k");
            for (int i = 1; i < array.length; i++) {
                System.out.print("\t");
                System.out.print(i);
            }

            System.out.println();
        }

    }

}
