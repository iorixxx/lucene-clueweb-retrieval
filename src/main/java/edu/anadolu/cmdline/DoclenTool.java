package edu.anadolu.cmdline;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.stats.DocLengthStats;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * A Tool That Computes Document Length Statistics
 */
public final class DoclenTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public String getShortDescription() {
        return "Computes Document Length Statistics";
    }


    @Override
    public void run(Properties props) throws Exception {

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        List<InfoNeed> needs = new ArrayList<>();
        for (Track track : dataset.tracks())
            needs.addAll(track.getTopics());


        final String[] fields = props.getProperty("freq.fields", "description,keywords,title,contents").split(",");

        for (final Path indexPath : discoverIndexes(dataset)) {
            Tag t = Tag.tag(indexPath.getFileName().toString());
            if (Files.notExists(dataset.collectionPath().resolve("stats").resolve(t.toString())))
                Files.createDirectories(dataset.collectionPath().resolve("stats").resolve(t.toString()));
            Set<String> words = distinctTerms(needs, Analyzers.analyzer(t));
            for (String field : fields)
                try (DocLengthStats docLengthStats = new DocLengthStats(dataset.collectionPath(), indexPath, field)) {
                    docLengthStats.process(words);
                }
        }
    }
}
