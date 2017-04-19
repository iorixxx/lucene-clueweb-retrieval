package edu.anadolu.cmdline;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Optimizes (force merge) indexes
 */
public final class OptimizeTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Optimize Lucene Indexes";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        String tfd_home = props.getProperty("tfd.home");
        if (tfd_home == null) {
            System.out.println("tfd.home is mandatory for optimize tool!");
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        if (dataset == null) {
            System.out.println(collection + " returned null dataset. Optimizing waterloo spam indices instead....");
            optimize(Paths.get(props.getProperty("waterloo.spam.CW09.lucene")));
            System.out.println("=================================");
            optimize(Paths.get(props.getProperty("waterloo.spam.CW12.lucene")));
            System.out.println("=================================");
            return;
        }


        for (Path path : discoverIndexes(dataset)) {
            optimize(path);
            System.out.println("=================================");
        }
    }

    private void optimize(Path indexPath) throws IOException {

        final long start = System.nanoTime();
        System.out.println("Opening Lucene index directory '" + indexPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig();

        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        iwc.setRAMBufferSizeMB(1024);
        iwc.setUseCompoundFile(false);

        try (IndexWriter writer = new IndexWriter(dir, iwc)) {
            // This can be a terribly costly operation, so generally it's only worth it when your index is static.
            writer.forceMerge(1);
        }

        dir.close();

        System.out.println("Optimization completed in " + execution(start));
    }
}
