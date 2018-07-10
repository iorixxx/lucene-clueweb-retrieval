package edu.anadolu.cmdline;

import edu.anadolu.eval.Evaluator;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Analysis on Result List returned by different models
 */
public final class ResultListTool extends CmdLineTool {

    @Override
    public String getShortDescription() {
        return "Displays corresponding excerpt from verbose_runs";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-qid", metaVar = "[1 to 200]", required = true, usage = "Query ID")
    private int qid;

    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "DPH_LGDL2_DFIC";

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = "KStem";

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    String op = "AND";

    @Override
    public void run(Properties props) throws Exception {

        final String home = props.getProperty("tfd.home");

        if (home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;

        if (qid < 0 || qid > 200) {
            System.out.println("Invalid qid option:" + qid + "! Query ID must be between 1 and 200");
            return;
        }

        //TODO find a way to convert query id to track
        Path thePath = Paths.get(home, "verbose_runs", tag /*, factory.track(qid).toString()*/);

        if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
            throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

        List<Path> paths = Evaluator.discoverTextFiles(thePath, op + "_all.txt");

        if (paths.size() == 0)
            throw new IllegalArgumentException(thePath + " does not contain any text files.");

        List<Path> pathList = percolate(paths, models);

        for (Path p : pathList) {
            list(p);
            System.out.println("============================================================");
        }
    }


    private void list(Path p) throws IOException {

        String queryID = Integer.toString(qid) + "\t";

        int counter = 0;
        for (String line : Files.readAllLines(p, StandardCharsets.US_ASCII)) {
            if (!line.startsWith(queryID)) continue;

            if (counter < 20) {
                System.out.println(line);
                counter++;
            } else return;
        }
    }

    public static List<Path> percolate(List<Path> paths, String models) {


        List<String> parts = new ArrayList<>();

        if (models.contains("_"))
            parts = Arrays.asList(models.split("_"));
        else if (models.endsWith("*"))
            parts.add(models);
        else if ("all".equals(models)) {

            parts.add("DFIC");
            parts.add("DPH");
            parts.add("DFRee");
            parts.add("DLH13");


        }

        if (parts.isEmpty()) throw new RuntimeException("parts is empty");

        List<Path> r = new ArrayList<>();
        for (Path path : paths) {
            String s = Evaluator.getRunTag(path);
            System.out.println("runTag " + s);
            for (String part : parts)
                if (part.endsWith("*")) {
                    if (s.startsWith(part.substring(0, part.length() - 1)))
                        r.add(path);
                } else if (s.startsWith(part + "_"))
                    r.add(path);

        }
        return r;
    }
}