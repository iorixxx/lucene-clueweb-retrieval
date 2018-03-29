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

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static edu.anadolu.eval.Evaluator.discoverTextFiles;
import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Tool for Sampling phase of learning to rank.
 * Saves sample list of 8 models decorated with relevance labels.
 * 0 is used for un-judged documents.
 */
public class SampleTool extends CmdLineTool {

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


        String runsDirectory = spam == 0 ? "runs" : "spam_" + spam + "_runs";


        Path samplePath = dataSet.collectionPath().resolve("samples");
        if (!Files.exists(samplePath))
            Files.createDirectory(samplePath);


        for (Track track : dataSet.tracks()) {

            Path thePath = dataSet.collectionPath().resolve(runsDirectory).resolve(tag).resolve(track.toString());

            if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
                throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

            List<Path> paths = discoverTextFiles(thePath, "_OR_all.txt");

            for (String model : models) {

                Path outPath = samplePath.resolve(track.toString() + "." + Evaluator.prettyModel(model) + ".txt");
                PrintWriter out = new PrintWriter(Files.newBufferedWriter(outPath, StandardCharsets.US_ASCII));

                int c = 0;
                for (Path path : paths) {
                    if (path.getFileName().toString().startsWith(model + "_")) {
                        c++;
                        List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

                        for (String s : lines) {

                            String[] parts = whiteSpaceSplitter.split(s);

                            if (parts.length != 6)
                                throw new RuntimeException("submission file does not contain 6 columns " + s);

                            final String docId = parts[2];

                            final int qID = Integer.parseInt(parts[0]);

                            InfoNeed need = new InfoNeed(qID, "", track, Collections.emptyMap());

                            int i = needs.indexOf(need);
                            if (-1 == i) {
                                System.out.println("cannot find information need " + qID);
                                continue;
                            }
                            final int judge = needs.get(i).getJudgeMap().getOrDefault(docId, 0);
                            out.println(qID + " 0 " + docId + " " + Integer.toString(judge));

                        }
                    }
                }
                if (c != 1) throw new RuntimeException(c + " many files start with for model " + model);

                out.flush();
                out.close();
            }
        }


        for (String model : models) {

            Path unifiedPath = samplePath.resolve(dataSet.collection().toString() + "." + Evaluator.prettyModel(model) + ".txt");

            final PrintWriter out = new PrintWriter(Files.newBufferedWriter(unifiedPath, StandardCharsets.US_ASCII));

            for (Track track : dataSet.tracks()) {

                Path localPath = samplePath.resolve(track.toString() + "." + Evaluator.prettyModel(model) + ".txt");

                List<String> lines = Files.readAllLines(localPath, StandardCharsets.US_ASCII);

                for (String line : lines)
                    out.println(line);

                out.flush();
                lines.clear();

            }
            out.flush();
            out.close();
        }
    }
}
