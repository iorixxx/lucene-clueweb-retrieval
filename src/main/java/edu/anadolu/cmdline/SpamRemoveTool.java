package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.spam.SpamRemovingIndexer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.kohsuke.args4j.Option;

import java.nio.file.Paths;
import java.util.Properties;

import static edu.anadolu.analysis.Tag.KStem;
import static edu.anadolu.cmdline.SpamTool.getSpamSolr;

/**
 * Indexer that removes spam during indexing
 */
public final class SpamRemoveTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    private int spam = 0;

    @Override
    public String getShortDescription() {
        return "Indexer Tool that removes spam during indexing for the ClueWeb09 dataset";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }

    @Override
    public void run(Properties props) throws Exception {

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;

        if (Collection.MQ07.equals(collection) || Collection.MQ08.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {
            System.out.println("No need to run separate indexer for Million Query!");
            return;
        }

        final String docsPath = props.getProperty("paths.docs." + collection.toString());

        final String indexPath = Paths.get(tfd_home, collection.toString(), "indexes").toString();


        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();

        if (docsPath == null || indexPath == null) {
            System.out.println(getHelp());
            return;
        }

        final String solrBaseURL = props.getProperty("SOLR.URL");

        if (solrBaseURL == null) {
            System.out.println(getHelp());
            return;
        }

        final HttpSolrClient solr = getSpamSolr(collection, solrBaseURL);
        if (solr == null) return;


        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();
        Indexer.IndexerConfig config = new Indexer.IndexerConfig()
                .useAnchorText(false)
                .useMetaFields(false)
                .useScripts(false)
                .useSemanticElements(false);
        SpamRemovingIndexer indexer = new SpamRemovingIndexer(dataset, docsPath, indexPath, solr, KStem, config, spam);
        int numIndexed = indexer.indexParallel(numThreads);

        System.out.println("Total " + numIndexed + " documents indexed in " + execution(start));
    }
}
