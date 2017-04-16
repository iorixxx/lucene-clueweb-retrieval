package edu.anadolu.exp;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Random statistics for presentation
 */
public class Presentation {

    static final WilcoxonSignedRankTest wilcoxonSignedRankTest = new WilcoxonSignedRankTest(/**NaNStrategy.FIXED, TiesStrategy.RANDOM**/);

    static final TTest tTest = new TTest();

    static final String home = "/Users/iorixxx/clueweb09";
    static final String tag = "KStemAnalyzer";
    static final String allModels = "DFIC_DLH13_LGDc16.25_BM25k2.425b0.2_DPH_DFRee_PL2c18.5";

    static void sensitivity()

    {

        double[] DLH13 = {0.092309898, 0.103119645, 0.107005381, 0.109185685, 0.109520609, 0.112787919, 0.115063401, 0.118895025, 0.124856853, 0.126059949, 0.128823604, 0.128818376, 0.127152132, 0.126860152, 0.117585533, 0.108160508, 0.100700761, 0.092309594};
        double[] DFRee = {0.110821929, 0.127920964, 0.133579898, 0.134558985, 0.135376751, 0.137884873, 0.140090305, 0.143834162, 0.148743959, 0.148796802, 0.148817766, 0.146763401, 0.144531421, 0.145168325, 0.137029036, 0.126136142, 0.115741523, 0.109560558};
        double[] DPH = {0.125754467, 0.15184203, 0.158800914, 0.160271574, 0.16126665, 0.165971827, 0.16653, 0.170718934, 0.175215076, 0.175368579, 0.175931675, 0.172244619, 0.168914569, 0.168250863, 0.159610609, 0.146700305, 0.13565401, 0.123453756};
        double[] BM25 = {0.11177066, 0.146933756, 0.154455025, 0.156499645, 0.156148376, 0.162105178, 0.164497817, 0.168735584, 0.173121168, 0.174586802, 0.17659736, 0.174709848, 0.17231599, 0.172714873, 0.164609492, 0.153547157, 0.141297157, 0.123011269};
        double[] Dirichlet = {0.096925787, 0.133222183, 0.141587919, 0.143845584, 0.145295431, 0.147368883, 0.15095335, 0.156634924, 0.161848782, 0.165953756, 0.166878173, 0.167673249, 0.16835599, 0.164883503, 0.155761472, 0.147179797, 0.135283655, 0.12249269};
        double[] LGD = {0.100632284, 0.137083655, 0.145171168, 0.147671675, 0.148528426, 0.150127665, 0.15404934, 0.159173604, 0.164702234, 0.167492944, 0.168943706, 0.168175279, 0.168005178, 0.164662995, 0.15439533, 0.144881472, 0.132585178, 0.121758223};
        double[] PL2 = {0.101276904, 0.139490609, 0.145907919, 0.149319289, 0.150869949, 0.153304518, 0.157828071, 0.163520812, 0.169062944, 0.172131269, 0.174433553, 0.17473934, 0.173354315, 0.171256599, 0.162823096, 0.153825685, 0.141690051, 0.129195076};
        double[] DFIC = {0.090888528, 0.149142995, 0.15663264, 0.162881827, 0.166067614, 0.168882335, 0.174029036, 0.179743909, 0.185879645, 0.187292893, 0.189934416, 0.189780457, 0.191345888, 0.191613909, 0.184625279, 0.172493299, 0.159031929, 0.141548782};

        double[] TEST = new double[DFIC.length];
        for (int i = 0; i < DFIC.length; i++)
            TEST[i] = DFIC[i] + 0.5;

        Map<String, double[]> map = new LinkedHashMap<>();

        map.put("DLH13", DLH13);
        map.put("DFRee", DFRee);
        map.put("DPH", DPH);
        map.put("BM25", BM25);
        map.put("Dirichlet", Dirichlet);
        map.put("LGD", LGD);
        map.put("PL2", PL2);
        map.put("DFIC", DFIC);
        map.put("TEST", TEST);


        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            String model = entry.getKey();
            double[] array = entry.getValue();

            System.out.println(model + "\t" + var(array) + "\t" + var2(array) + "\t" + var3(array) + "\t" + var4(array) + "\t" + var5(array));
        }

    }

    /**
     * http://www.mathsisfun.com/data/standard-deviation.html
     *
     * @param array
     * @return
     */

    static double var(double[] array) {
        double mean = StatUtils.mean(array);

        double var = 0.0;
        for (double d : array) {
            var += Math.pow(mean - d, 2);
        }

        return Math.sqrt(var / array.length);
    }

    static double var2(double[] array) {
        double mean = StatUtils.mean(array);

        double var = 0.0;
        for (double d : array) {
            var += (Math.abs(mean - d) / mean);
        }

        return Math.sqrt(var / array.length);
    }

    static double var3(double[] array) {
        double mean = StatUtils.mean(array);

        double var = 0.0;
        for (double d : array) {
            var += Math.pow((mean - d) / mean, 2);
        }

        return Math.sqrt(var / array.length);
    }

    public static double var4(double[] array) {
        double mean = StatUtils.mean(array);

        double var = 0.0;
        for (double d : array) {
            var += Math.pow((mean - d) / mean * 100, 2);
        }

        return Math.sqrt(var / array.length);
    }

    static double var5(double[] array) {
        double mean = StatUtils.mean(array);

        double var = 0.0;
        for (double d : array) {
            var += Math.pow((mean - d) / ((mean + d) / 2) * 100, 2);
        }

        return Math.sqrt(var / array.length);
    }

    /**
     * Add cartesian summary results to MS results proposed by He and Ounis (2004).
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        sensitivity();

        List<String> lines = Files.readAllLines(Paths.get("/Users/iorixxx/gd/AhmetPhDThesis/summary/summary.csv"));

        for (int i = 1; i < lines.size(); i++) {


            String line = lines.get(i);

            if (!line.startsWith("Cartesian")) continue;
            //   System.out.println(lines.get(i));

            String[] parts = line.split(",");

            for (int k = 2; k <= 10; k++)
                System.out.println("SEL," + k + "," + parts[2] + "," + parts[5] + "," + parts[6] + "," + parts[7]);
        }


    }

//    public static void facetOnQueryLength() {
//
//        QueryBank bank = new QueryBank(home);
//
//
//        for (int minRel : new int[]{0, 27, 69}) {
//
//            List<InfoNeed> infoNeedList = Collections.unmodifiableList(bank.getAllQueries(minRel));
//
//            Multiset<String> multiset = HashMultiset.create();
//
//            for (InfoNeed need : infoNeedList) {
//
//                multiset.add(Integer.toString(need.wordCount()));
//            }
//
//
//            double one = multiset.count("1") * 100 / infoNeedList.size();
//
//            double two = multiset.count("2") * 100 / infoNeedList.size();
//
//            System.out.println("minRel=" + minRel + " " + multiset + " one = " + one + "% two = " + two + "%");
//        }
//
//
//    }
//
//    public static void facetOnQueryType() {
//
//
//        QueryBank bank = new QueryBank(home);
//
//
//        for (int minRel : new int[]{0, 27, 69}) {
//
//            List<InfoNeed> infoNeedList = Collections.unmodifiableList(bank.getAllQueries(minRel));
//
//            Multiset<String> multiset = HashMultiset.create();
//
//            for (InfoNeed need : infoNeedList) {
//                multiset.add(need.type());
//            }
//
//
//            double ambiguous = multiset.count("ambiguous") * 100 / infoNeedList.size();
//
//            double faceted = multiset.count("faceted") * 100 / infoNeedList.size();
//
//            System.out.println("minRel=" + minRel + " " + multiset + " ambiguous = " + ambiguous + "% faceted = " + faceted + "%");
//        }
//
//
//    }
//
//    public static ModelScore oracle(Metric metric, int depth, String models, String op) {
//
//
//        QueryBank bank = new QueryBank(home);
//
//        List<InfoNeed> infoNeedList = Collections.unmodifiableList(bank.getAllQueries(0));
//
//        Evaluator evaluator = new Evaluator(home, infoNeedList, tag, metric, depth, models, "evals", op);
//
//        return evaluator.oracleMax();
//    }
//
//    public static void oracle(int minRel, Metric metric, int depth, String models, String op) {
//
//
//        QueryBank bank = new QueryBank(home);
//
//        List<InfoNeed> infoNeedList = Collections.unmodifiableList(bank.getAllQueries(minRel));
//
//
//        Evaluator evaluator = new Evaluator(home, infoNeedList, tag, metric, depth, models, "evals", op);
//
//
//        final ModelScore oracle = evaluator.oracleMax();
//
//        List<ModelScore> modelMeans = evaluator.averageForAllModels(infoNeedList);
//        Collections.sort(modelMeans);
//        ModelScore best = modelMeans.get(0);
//
//        System.out.println(minRel + "\t" + Double.toString(best.score).replaceAll("\\.", ",") + "\t" + Double.toString(oracle.score).replaceAll("\\.", ","));
//    }
//
//    public static List<String> modelCombinations(int n) {
//
//        List<String> list = new ArrayList<>();
//
//        // Create the initial vector
//        ICombinatoricsVector<String> initialVector = Factory.createVector(
//                allModels.split("_")
//
//        );
//
//        // Create a simple combination generator to generate 3-combinations of the initial vector
//        Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, n);
//
//
//        // Print all possible combinations
//        for (ICombinatoricsVector<String> combination : gen) {
//
//            StringBuilder builder = new StringBuilder();
//
//            for (int i = 0; i < n; i++)
//                builder.append(combination.getValue(i)).append("_");
//
//            builder.deleteCharAt(builder.length() - 1);
//            //  System.out.println(builder.toString());
//            list.add(builder.toString());
//
//        }
//
//        return list;
//    }
//
//    public static double[] getScoreArray(String models) {
//
//        QueryBank bank = new QueryBank(home);
//
//        List<InfoNeed> infoNeedList = Collections.unmodifiableList(bank.getAllQueries(0));
//
//        Evaluator evaluator = new Evaluator(home, infoNeedList, tag, Metric.ERR, 20, models, "evals", "AND");
//        return evaluator.oracleScoreArray();
//    }
//
//    public static double[] bestScoreArray(int n) {
//        return getScoreArray(bestOracle(n).model);
//    }
//
//    public static double[] worseScoreArray(int n) {
//        return getScoreArray(worseOracle(n).model);
//    }
//
//    public static ModelScore bestOracle(int n) {
//        List<ModelScore> modelScoreList = oracleList(n);
//        Collections.sort(modelScoreList);
//        return modelScoreList.get(0);
//    }
//
//    public static ModelScore worseOracle(int n) {
//        List<ModelScore> modelScoreList = oracleList(n);
//        Collections.sort(modelScoreList);
//        return modelScoreList.get(modelScoreList.size() - 1);
//    }
//
//    public static List<ModelScore> oracleList(int n) {
//        List<String> modelList = modelCombinations(n);
//
//        List<ModelScore> modelScoreList = new ArrayList<>();
//
//        for (String models : modelList) {
//            ModelScore oracle = oracle(Metric.ERR, 20, models, "AND");
//            modelScoreList.add(new ModelScore(models, oracle.score));
//        }
//
//        return modelScoreList;
//    }
//
//    public static void main(String[] args) {
//
//        facetOnQueryLength();
//
//        for (int minRel = 0; minRel < 100; minRel++)
//            oracle(minRel, Metric.ERR, 20, allModels, "AND");
//
//
//        for (int n = 7; n > 2; n--) {
//
//            List<ModelScore> modelScoreList = oracleList(n);
//
//            Collections.sort(modelScoreList);
//
//            System.out.println("************ n = " + n + " ***************");
//            for (int i = 0; i < Math.min(5, modelScoreList.size()); i++)
//                System.out.println(modelScoreList.get(i));
//
//        }
//
//
//        System.out.println("========== Significance Tests ================");
//        double[] d1 = bestScoreArray(7);
//        double[] d2 = worseScoreArray(6);
//
//
//        double tP = tTest.pairedTTest(d1, d2) / 2d;
//        double wP = wilcoxonSignedRankTest.wilcoxonSignedRankTest(d1, d2, false);
//        System.out.println("best of 7 versus worst of 6 " + "(tp:" + String.format("%.5f", tP) + "; wp:" + String.format("%.5f", wP) + ")");
//
//    }
}
