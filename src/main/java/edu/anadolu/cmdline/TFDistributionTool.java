package edu.anadolu.cmdline;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Term Frequency Distribution Analysis Tool
 */
public final class TFDistributionTool extends CmdLineTool {

    @Option(name = "-numBins", required = false, usage = "number of bins")
    protected int numBins = 1000;

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Option(name = "-tag", required = false, usage = "If you want to use specific tag, e.g. KStemField")
    private String tag = null;

    @Override
    public String getShortDescription() {
        return "Term Frequency Distribution Analysis Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.qrels freq.fields paths.indexes paths.freqs";
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

        String[] fields = props.getProperty("freq.fields", "anchor,description,keywords,title,contents").split(",");

        final Path freqsPath = dataset.collectionPath().resolve("freqs");

        final BinningStrategy binningStrategy = new LengthNormalized(numBins);

        List<Path> indexList = discoverIndexes(dataset);

        if ("query".equals(task)) {

            // 1-) QueryFreqDist
            long start = System.nanoTime();
            List<InfoNeed> needs = new ArrayList<>();
            for (Track track : dataset.tracks())
                needs.addAll(track.getTopics());

            for (Path indexPath : indexList) {
                try (FSDirectory directory = FSDirectory.open(indexPath); IndexReader reader = DirectoryReader.open(directory)) {
                    String indexTag = indexPath.getFileName().toString();
                    for (String field : fields) {
                        QueryFreqDistribution queryFreqDistribution = new QueryFreqDistribution(reader, freqsPath, binningStrategy, field, indexTag);
                        queryFreqDistribution.process(needs, indexTag, 10000);
                    }
                }
            }
            System.out.println("Query Frequency Distribution extraction finished in " + execution(start));
            return;
        }


        long start = System.nanoTime();
        for (Path indexPath : indexList) {

            try (final IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
                System.out.println("Term Freq. Dist opened index directory : " + indexPath + " has " + reader.numDocs() + " numDocs and has " + reader.maxDoc() + " maxDocs");
                final String indexTag = indexPath.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !indexTag.equals(this.tag)) continue;

                Tag tag = Tag.tag(indexTag);
                System.out.println("analyzer tag " + tag);
                Analyzer analyzer = Analyzers.analyzer(tag);

                for (String field : fields) {

                    final TFD distribution;
                    if ("diri".equals(task)) {
                        distribution = new DirichletDistribution(reader, binningStrategy, field, analyzer);
                    } else if ("zero".equals(task)) {
                        distribution = new ZeroDistribution(reader, binningStrategy, field, analyzer);
                    } else if ("phi".equals(task)) {
                        distribution = new Phi(reader, binningStrategy, field, analyzer);
                    } else if ("term".equals(task)) {
                        distribution = new TermFreqDistribution(reader, binningStrategy, field, analyzer);
                    } else {
                        System.out.println("Unknown task : " + task);
                        return;
                    }

                    for (final Track track : dataset.tracks()) {

                        final Path path = Paths.get(freqsPath.toString(), indexTag, track.toString());

                        if (!Files.exists(path))
                            Files.createDirectories(path);

                        distribution.processSingeTrack(track, path);

                    }
                }
            }
        }
        System.out.println(task + " Frequency Distribution extraction finished in " + execution(start));
    }
}
