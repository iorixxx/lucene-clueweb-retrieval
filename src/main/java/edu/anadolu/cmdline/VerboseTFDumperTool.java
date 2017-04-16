package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.*;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Verbose Term Frequency Dumper Tool
 */
public final class VerboseTFDumperTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Verbose Term Frequency Dumper Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home docCount termCount";
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

        if (dataset == null) {
            System.out.println(collection + " returned null dataset");
            return;
        }


        List<InfoNeed> needs = new ArrayList<>();
        for (Track track : dataset.tracks())
            needs.addAll(track.getTopics());

        Set<String> words = distinctTerms(needs);

        for (final Path indexPath : discoverIndexes(dataset)) {
            VerboseTFDumper verboseTFDumper = new VerboseTFDumper(dataset.collectionPath(), indexPath, words);
            verboseTFDumper.saveTermFrequenciesForOneTermQueries("contents", dataset.tracks());
            verboseTFDumper.saveTermFrequenciesForAllQueries("contents");

            TFNormalization[] normalizations = {new L0(), new L1(verboseTFDumper.getD_BAR()), new L2(verboseTFDumper.getD_BAR())};

            for (TFNormalization normalization : normalizations)
                verboseTFDumper.saveNormalizedTermFrequenciesForAllQueries("contents", normalization);

            verboseTFDumper.close();

        }
    }
}
