package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
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

        if (dataset == null) {
            System.out.println(collection + " returned null dataset");
            return;
        }


        int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));

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
                    for (String field : fields) {
                        QueryFreqDistribution queryFreqDistribution = new QueryFreqDistribution(reader, freqsPath, binningStrategy, field);
                        queryFreqDistribution.process(needs, indexPath.getFileName().toString(), 10000);
                    }
                }
            }
            System.out.println("Query Frequency Distribution extraction finished in " + execution(start));
        }


        // 2-) FreqDist over Result List
//        start = System.nanoTime();
//        for (Path indexPath : indexList)
//            TermFreqDistribution.saveTermFreqDistOverResultList(indexPath, dataset.tracks(), tfd_home, Models.values());
//        System.out.println("Frequency Distribution over Result List finished in " + execution(start));


        if ("term".equals(task)) {
            // 3-) Good old FreqDist
            long start = System.nanoTime();
            for (Path indexPath : indexList)
                for (String field : fields)
                    TermFreqDistribution.mainWithThreads(binningStrategy, dataset.tracks(), field, indexPath, freqsPath, numThreads);
            System.out.println("Term Frequency Distribution extraction finished in " + execution(start));
        }


        if ("phi".equals(task)) {
            // 3-) Phi FreqDist
            long start = System.nanoTime();
            for (Path indexPath : indexList)
                for (String field : fields)
                    Phi.mainWithThreads(binningStrategy, dataset.tracks(), field, indexPath, freqsPath, numThreads);
            System.out.println("Phi Term Frequency Distribution extraction finished in " + execution(start));
        }
    }
}
