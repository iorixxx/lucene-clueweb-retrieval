package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Optimizes (force merge) indexes
 */
final class OptimizeTool extends CmdLineTool {

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

        for (Path path : discoverIndexes(dataset)) {
            optimize(path);
            System.out.println("=================================");
        }
    }

    private void optimize(Path indexPath) throws IOException {

        final long start = System.nanoTime();
        System.out.println("Opening Lucene index directory '" + indexPath.toAbsolutePath() + "'...");


        final IndexWriterConfig iwc = new IndexWriterConfig();

        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        iwc.setRAMBufferSizeMB(1024);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        try (final Directory dir = FSDirectory.open(indexPath);
             IndexWriter writer = new IndexWriter(dir, iwc)) {
            // This can be a terribly costly operation, so generally it's only worth it when your index is static.
            writer.forceMerge(1);

            if (writer.maxDoc() == writer.numDocs())
                System.out.println("Number of documents: " + writer.numDocs());
            else
                System.out.println("numDocs: " + writer.maxDoc() + " maxDocs = " + writer.numDocs());

        } catch (IndexNotFoundException e) {
            System.out.println("IndexNotFound in " + indexPath.toAbsolutePath());
        }

        System.out.println("Optimization completed in " + execution(start));
    }
}
