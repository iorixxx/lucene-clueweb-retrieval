package edu.anadolu.cmdline;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.Aggregate;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DLH13;
import edu.anadolu.similarities.DPH;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.search.similarities.ModelBase;
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
public class EvaluatorJudgeTool extends CmdLineTool {

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

    @Option(name = "-tag", metaVar = "[KStem KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0";


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
        Track[] tracks = dataSet.tracks();
        final Set<ModelBase> modelBaseSet = Arrays.stream(models.split("_"))
                .map(ParamTool::string2model)
                .collect(Collectors.toSet());

        modelBaseSet.add(new DFIC());
        modelBaseSet.add(new DPH());
        modelBaseSet.add(new DLH13());
        modelBaseSet.add(new DFRee());

        String runsDirectory = spam == 0 ? "runs" : "spam_" + spam + "_runs";

        Map<ModelBase, List<String>> map = new HashMap<>();

        for (ModelBase model : modelBaseSet) {
            map.put(model, new ArrayList<>());
        }


        for (Track track : dataSet.tracks()) {

            Path thePath = dataSet.collectionPath().resolve(runsDirectory).resolve(tag).resolve(track.toString());

            if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
                throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

            List<Path> paths = discoverTextFiles(thePath, "_OR_all.txt");

            for (ModelBase model : modelBaseSet) {
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


        for (ModelBase model : modelBaseSet) {
            List<String> lines = map.get(model);
            double[] JORall = new double[dataSet.getTopics().size()]; // Judge Over Retrieved for each need
            Arrays.fill(JORall, 0);
            int ja=0;

            for(Track t: tracks) {
                List<InfoNeed> needs = t.getTopics();

                double[] JORs = new double[needs.size()]; // Judge Over Retrieved for each need
                Arrays.fill(JORs, 0);

                int i = 0;
                for (InfoNeed need : needs) {

                    int judges = 0;

                    List<String> l = lines.stream().filter(s -> s.startsWith(need.id() + "\tQ0\t")).collect(Collectors.toList());

                    int c = 1;
                    for (String s : l) {

                        String[] parts = whiteSpaceSplitter.split(s);

                        if (parts.length != 6)
                            throw new RuntimeException("submission file does not contain 6 columns " + s);

                        int rank = Integer.parseInt(parts[3]);

                        if (rank != c++) throw new RuntimeException("rank " + rank + " different from counter " + c);

                        String docId = parts[2];
                        int j = need.getJudge(docId);
                        if (j != -5) judges++; //judged
                    }

                    JORs[i++] = (double) judges / (c - 1);
                    JORall[ja++] = (double) judges / (c - 1);
                }
                System.out.println(model.toString() + "\t" + t.toString() + "\t"+ StatUtils.mean(JORs));
            }
            System.out.println(model.toString() + "\t" + "ALL" + "\t" + StatUtils.mean(JORall));
        }
    }

}
