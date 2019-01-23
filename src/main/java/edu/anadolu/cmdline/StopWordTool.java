package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.BinningStrategy;
import edu.anadolu.freq.LengthNormalized;
import edu.anadolu.freq.TermFreqDistribution;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Stop Word Frequency Distribution Analysis Tool
 */
public final class StopWordTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Stop Word Distribution Analysis Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " freq.fields paths.indexes paths.freqs";
    }

    static Set<String> getStopWords(Properties props) {

        final Set<String> stopWords = new HashSet<>();

        if (props.getProperty("stop.words") == null) {
            System.out.println("Falling back to default stop words set for English since stop.words property is not defined in config.properties");
            for (Object o : EnglishAnalyzer.ENGLISH_STOP_WORDS_SET) {
                stopWords.add(new String((char[]) o));
            }
        } else
            stopWords.addAll(Arrays.asList(props.getProperty("stop.words").split(",")));

        return stopWords;
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        String tfd_home = props.getProperty("tfd.home");
        if (tfd_home == null) {
            System.out.println("tfd.home is mandatory for query statistics!");
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        final long start = System.nanoTime();

        final Set<String> stopWords = getStopWords(props);

        final String[] fields = props.getProperty("freq.fields", "anchor,description,keywords,title,contents").split(",");


        final Path freqsPath = Paths.get(tfd_home, collection.toString(), "freqs");


        final BinningStrategy binningStrategy = new LengthNormalized(1000);

        for (Path indexPath : discoverIndexes(dataset))
            for (String field : fields) {
                TermFreqDistribution.mainForStopWords(binningStrategy, field, indexPath, freqsPath, stopWords);
            }

        System.out.println("StopWord completed in " + execution(start));

    }
}


