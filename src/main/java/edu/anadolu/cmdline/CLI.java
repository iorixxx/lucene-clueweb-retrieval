package edu.anadolu.cmdline;

import edu.anadolu.field.CrossTool;
import edu.anadolu.field.FieldTool;
import edu.anadolu.field.SelectiveStemmingTool;
import edu.anadolu.knn.*;
import edu.anadolu.ltr.QDFeatureTool;
import edu.anadolu.ltr.SEOTool;
import org.apache.lucene.util.Version;
import org.clueweb09.WarcTool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Command Line Interface
 */
public final class CLI {

    public static final String CMD = "run.sh";

    private static Map<String, CmdLineTool> toolLookupMap;

    static {
        toolLookupMap = new LinkedHashMap<>();

        List<CmdLineTool> tools = new LinkedList<>();

        // Anchor Text Extraction
        tools.add(new AnchorExtractorTool());

        // Indexing
        tools.add(new IndexerTool());

        // Searching
        tools.add(new SearcherTool());

        // Evaluation
        tools.add(new EvaluatorTool());

        // Term Frequency Distribution Analysis
        tools.add(new TFDistributionTool());

        // Stop Word Frequency Distribution Analysis
        tools.add(new StopWordTool());

        // ClueWeb09 Collection Statistic Tool
        tools.add(new StatsTool());

        // Tool for integration Waterloo Spam Rankings
        tools.add(new SpamTool());

        // Verbose Term Frequency Dumper Tool
        tools.add(new VerboseTFDumperTool());

        // Export Tool
        tools.add(new ExportTool());

        // KNN Tool
        tools.add(new KNNTool());

        // MySQL Tool
        tools.add(new MySQLTool());

        // LaTex Tool
        tools.add(new LatexTool());

        // Free-Parameter Determining Tool
        tools.add(new ParamTool());

        // Document Length Tool
        tools.add(new DoclenTool());

        // Optimize (force merge) Tool
        tools.add(new OptimizeTool());

        // Rule Based (RB) Tool
        tools.add(new RBTool());

        // Tool for Learning to Select (LTS) framework
        tools.add(new LTSTool());

        // Tool for Model Selection (MS) framework
        tools.add(new MSTool());

        // Tool for Feature Extraction
        tools.add(new FeatureTool());

        tools.add(new SpamEvalTool());

        tools.add(new CacheTool());

        // Tool for Query2Query (Q2Q)
        tools.add(new Q2QTool());

        // Tool for Term2Term (T2T)
        tools.add(new T2TTool());

        tools.add(new AbsoluteTool());

        tools.add(new XTool());

        tools.add(new YTool());

        tools.add(new BinaryTool());

        tools.add(new GrandTool());

        tools.add(new ExampleTool());

        tools.add(new TFTool());

        tools.add(new FieldTool());

        tools.add(new CrossTool());

        tools.add(new HighTool());

        tools.add(new JudgeTool());

        tools.add(new SampleTool());

        tools.add(new SpamRemoveTool());

        tools.add(new SearchTool());

        tools.add(new SelectiveStemmingTool());

        tools.add(new WarcTool());

        tools.add(new QueryPerTFTool());

        tools.add(new RocTool());

        tools.add(new CustomTool());

        tools.add(new SEOTool());

        tools.add(new QDFeatureTool());

        tools.add(new WikiTool());

        tools.add(new CormakTool());

        tools.add(new DeepTool());

        tools.add(new EvaluatorJudgeTool());

        for (CmdLineTool tool : tools) {
            toolLookupMap.put(tool.getName(), tool);
        }

        toolLookupMap = Collections.unmodifiableMap(toolLookupMap);

        tools.clear();
    }

    private static void usage() {
        System.out.println("Lucene: " + Version.LATEST.toString());
        System.out.println("Usage: " + CMD + " TOOL");
        System.out.println("where TOOL is one of:");

        // distance of tool name from line start
        int numberOfSpaces = -1;
        for (String toolName : toolLookupMap.keySet()) {
            if (toolName.length() > numberOfSpaces) {
                numberOfSpaces = toolName.length();
            }
        }
        numberOfSpaces = numberOfSpaces + 4;

        for (CmdLineTool tool : toolLookupMap.values()) {

            System.out.print("  " + tool.getName());

            for (int i = 0; i < Math.abs(tool.getName().length() - numberOfSpaces); i++) {
                System.out.print(" ");
            }

            System.out.println(tool.getShortDescription());
        }
    }

    private static Properties readProperties() {

        Properties prop = new Properties();

        Path path = Paths.get("config.properties");

        if (Files.exists(path) && Files.isReadable(path))

            try (InputStream input = Files.newInputStream(Paths.get("config.properties"))) {
                prop.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        else
            try (InputStream input = CLI.class.getClassLoader().getResourceAsStream("config.properties")) {

                if (input == null) {
                    System.out.println("Sorry, unable to find config.properties in class path");
                    return null;
                }
                prop.load(input);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        return prop;
    }

    public static void main(String[] args) throws Exception {

        System.out.println();

        if (args.length == 0) {
            usage();
            System.exit(0);
        }

        String toolName = args[0];

        CmdLineTool tool = toolLookupMap.get(toolName);

        if (null == tool) {
            throw new RuntimeException("Tool " + toolName + " is not found.");
        }

        Properties properties = readProperties();

        if (args.length > 1) {

            StringBuilder buffer = new StringBuilder();

            for (int i = 1; i < args.length; i++) {
                buffer.append(args[i]).append(",");
            }

            buffer.delete(buffer.length() - 1, buffer.length());
            if (properties != null)
                properties.put("toolArguments", buffer.toString());
        }

        tool.do_run(properties);
    }
}
