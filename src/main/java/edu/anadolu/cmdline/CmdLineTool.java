package edu.anadolu.cmdline;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.Freq;
import edu.anadolu.knn.Winner;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.lucene.analysis.Analyzer;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A simple tool which can be executed from the command line.
 * <p>
 * <b>Note:</b> Do not use this class, internal use only!
 */

public abstract class CmdLineTool {

    public final static String[] parametricModels = {"BM25", "LGD", "PL2", "DirichletLM"};

    final static String[] tags = {"KStem", "KStemAnchor"};
    protected final static String[] operators = {"OR"};


    public final static Freq[] types = {Freq.Rel};

    protected final static int[] bins = {500, 1000};

    protected final static Winner[] winners = {Winner.Single, Winner.Multi};

    protected int parseArguments(Properties props) {

        CmdLineParser parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(90));

        if (props.get("toolArguments") == null) {
            return 1;
        }

        String[] toolArguments = props.get("toolArguments").toString().split(",");

        try {
            parser.parseArgument(toolArguments);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println("Example: Param" + parser.printExample(OptionHandlerFilter.ALL));
            return -1;
        }

        return 0;
    }

    static synchronized Properties cacheProperties(DataSet dataSet) {
        Path cacheFile = dataSet.collectionPath().resolve("cache.properties");
        return cacheProperties(cacheFile);
    }

    static synchronized Properties cacheProperties(Path cacheFile) {
        if (!Files.exists(cacheFile))
            try {
                Files.createFile(cacheFile);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            properties.load(reader);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return properties;
    }

    static synchronized void saveCacheProperties(DataSet dataSet, Properties properties) {

        Path cacheFile = dataSet.collectionPath().resolve("cache.properties");

        try (OutputStream out = Files.newOutputStream(cacheFile)) {
            properties.store(out, "best parameters of " + dataSet.toString());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    protected String tfd_home;

    /**
     * Executes the tool with the given properties.
     *
     * @param props properties
     */
    public void do_run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home;

        if (props.getProperty("tfd.home") == null) {
            System.out.println("tfd.home property is null. Trying with " + System.getProperty("user.home") + File.separator + "TFD_HOME");
            tfd_home = System.getProperty("user.home") + File.separator + "TFD_HOME";
        } else {
            tfd_home = props.getProperty("tfd.home").replaceFirst("^~", System.getProperty("user.home"));
        }

        Path p = Paths.get(tfd_home);

        if (Files.exists(p) && Files.isDirectory(p)) {
            this.tfd_home = p.toAbsolutePath().toString();
            props.setProperty("tfd.home", this.tfd_home);
            run(props);
        } else {
            System.out.println("TFD_HOME directory does not exist: " + p.toAbsolutePath());
            System.out.println(getHelp());
        }
    }

    /**
     * Executes the tool with the given properties.
     *
     * @param props properties
     */
    public abstract void run(Properties props) throws Exception;

    /**
     * Retrieves the name of the training data tool.
     * The name (used as command) must not contain white spaces.
     *
     * @return the name of the command line tool
     */
    public String getName() {
        if (getClass().getName().endsWith("Tool")) {
            return getClass().getSimpleName().substring(0, getClass().getSimpleName().length() - 4);
        } else {
            return getClass().getSimpleName();
        }
    }

    /**
     * Retrieves a description on how to use the tool.
     *
     * @return a description on how to use the tool
     */
    public abstract String getHelp();


    /**
     * Retrieves a short description of what the tool does.
     *
     * @return a short description of what the tool does
     */
    public abstract String getShortDescription();

    /**
     * Human readable execution time information
     *
     * @param startNano start of the task
     * @return human readable message
     */
    public static String execution(long startNano) {
        return DurationFormatUtils.formatDuration(TimeUnit.MILLISECONDS.convert(System.nanoTime() - startNano, TimeUnit.NANOSECONDS), "HH:mm:ss");
    }

    /**
     * Discovers Lucene index directories
     *
     * @param dataSet location where indices are stored
     * @return list of different Lucene indices available
     */
    protected List<Path> discoverIndexes(DataSet dataSet) {
        Path indexesPath = dataSet.indexesPath();
        List<Path> pathList = new ArrayList<>();

        if (!Files.exists(indexesPath) || !Files.isDirectory(indexesPath) || !Files.isReadable(indexesPath)) {
            throw new IllegalArgumentException(indexesPath + " does not exist or is not a directory.");
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexesPath, Files::isDirectory)) {
            for (Path path : stream) {
                // Iterate over the paths in the directory
                pathList.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return pathList;
    }


    Set<String> distinctTerms(List<InfoNeed> needs, Analyzer analyzer) {
        Set<String> set = new HashSet<>();
        for (InfoNeed need : needs)
            set.addAll(Analyzers.getAnalyzedTokens(need.query(), analyzer));
        return Collections.unmodifiableSet(set);
    }
}
