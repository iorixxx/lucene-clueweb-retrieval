package edu.anadolu.cmdline;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static edu.anadolu.eval.Evaluator.discoverTextFiles;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Returns judgements from the results lists of models
 */
public class JudgeTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "result list judgements";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;

    @Option(name = "-tag", metaVar = "[KStem KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();


    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-k", metaVar = "[20|100|...|1000]", required = false, usage = "Top-k")
    protected int k = 100;


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
        List<InfoNeed> needs = dataSet.getTopics();

        Set<String> models = new TreeSet<>();

        for (String parametricModel : parametricModels)
            models.add(ParamTool.train(parametricModel, dataSet, tag, measure, "OR").toString());

        System.out.println("========= best parameters ===========");
        System.out.println(models);

        models.add("DFIC");
        models.add("DPH");
        models.add("DFRee");
        models.add("DLH13");

        //   for (int spam = 0; spam < 100; spam += 5) {

        String runsDirectory = spam == 0 ? "runs" : "spam_" + spam + "_runs";

        Map<String, List<String>> map = new HashMap<>();

        for (String model : models) {
            map.put(model, new ArrayList<>());
        }


        for (Track track : dataSet.tracks()) {

            Path thePath = dataSet.collectionPath().resolve(runsDirectory).resolve(tag).resolve(track.toString());

            if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
                throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

            List<Path> paths = discoverTextFiles(thePath, "_OR_all.txt");

            for (String model : models) {
                int c = 0;
                for (Path path : paths) {
                    if (path.getFileName().toString().startsWith(model + "_")) {
                        c++;
                        map.get(model).addAll(Files.readAllLines(path, StandardCharsets.US_ASCII));
                    }
                }
                if (c != 1) throw new RuntimeException(c + " many files start with for model " + model);
            }
        }


        for (String model : models) {

            int[] radix = new int[10];
            Arrays.fill(radix, 0);
            List<String> lines = map.get(model);

            for (InfoNeed need : needs) {

                List<String> l = lines.stream().filter(s -> s.startsWith(need.id() + "\tQ0\t")).limit(k).collect(Collectors.toList());

                // if (l.size() < 100)
                //  System.out.println(model + " " + need.id() + " -> " + l.size());

                int c = 1;
                for (String s : l) {

                    String[] parts = whiteSpaceSplitter.split(s);

                    if (parts.length != 6)
                        throw new RuntimeException("submission file does not contain 6 columns " + s);

                    int rank = Integer.parseInt(parts[3]);

                    if (rank != c++) throw new RuntimeException("rank " + rank + " different from counter " + c);

                    String docId = parts[2];
                    int j = need.getJudge(docId) + 5;
                    radix[j]++;

                }
            }

            System.out.print(Evaluator.prettyModel(model) + "\t" + radix[0] + "\t" + radix[3]);

            for (int i = 0; i < 5; i++)
                System.out.print("\t" + radix[i + 5]);

            System.out.println();

            //System.out.print((radix[9] + radix[8] + radix[7] + radix[6]) + "\t");

        }

        System.out.println();
    }

}
