package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.exp.ROB04;
import edu.anadolu.mc.MCIndexer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.kohsuke.args4j.Option;

import java.nio.file.Paths;
import java.util.Properties;

import static edu.anadolu.analysis.Tag.KStem;

/**
 * Indexer Tool for ClueWeb09 ClueWeb12 Gov2 collections
 */
public final class IndexerTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-anchor", usage = "Boolean switch to index anchor text")
    private boolean anchor = false;

    @Option(name = "-field", usage = "Boolean switch to index different document representations")
    private boolean field = false;

    @Option(name = "-script", usage = "Boolean switch to index scripts as separate fields")
    private boolean script = false;

    @Option(name = "-semantic", usage = "Boolean switch to index HTML5 semantic elements")
    private boolean semantic = false;

    @Option(name = "-silent", usage = "Do not print the identifiers of empty documents that are skipped during indexing (which are printed by default)")
    private boolean silent = false;


    @Option(name = "-tag", metaVar = "[KStem|NoStem|ICU|NoStemTurkish|Zemberek]", required = false, usage = "Analyzer Tag")
    private Tag tag = KStem;

    @Override
    public String getShortDescription() {
        return "Indexer Tool for ClueWeb09 ClueWeb12 Gov2 collections";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (Collection.MQ07.equals(collection) || Collection.MQ08.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {
            System.out.println("No need to run separate indexer for Million Query!");
            return;
        }

        final String docsPath = props.getProperty("paths.docs." + collection.toString());

        final String indexPath = Paths.get(tfd_home, collection.toString(), "indexes").toString();

        /**
         * Indexer code snippet for Robust Track 2004
         */
        if (Collection.ROB04.equals(collection)) {

            final long start = System.nanoTime();
            final int numIndexed = ROB04.index(docsPath, indexPath, tag);
            System.out.println("Total " + numIndexed + " documents indexed in " + execution(start));
            return;
        }

        if (Collection.MC.equals(collection)) {
            final long start = System.nanoTime();
            final int numIndexed = MCIndexer.index(docsPath, indexPath, tag);
            System.out.println("Total " + numIndexed + " documents indexed in " + execution(start));
            return;
        }


        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();

        if (docsPath == null || indexPath == null) {
            System.out.println(getHelp());
            return;
        }

        final HttpSolrClient solr;
        if (anchor) {

            final String solrBaseURL = props.getProperty("SOLR.URL");
            if (solrBaseURL == null) {
                System.out.println(getHelp());
                return;
            }
            if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {
                solr = new HttpSolrClient.Builder().withBaseSolrUrl(solrBaseURL + "anchor09A").build();
            } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection))
                solr = new HttpSolrClient.Builder().withBaseSolrUrl(solrBaseURL + "anchor12A").build();
            else {
                System.out.println("anchor text is only available to ClueWeb09 and ClueWeb12 collections!");
                return;
            }
        } else {
            solr = null;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();
        Indexer.IndexerConfig config = new Indexer.IndexerConfig()
                .useAnchorText(anchor)
                .useMetaFields(field)
                .useScripts(script)
                .useSemanticElements(semantic)
                .useSilent(silent);
        Indexer indexer = new Indexer(dataset, docsPath, indexPath, solr, tag, config);
        int numIndexed = indexer.indexWithThreads(numThreads);
        System.out.println("Total " + numIndexed + " documents indexed in " + execution(start));
    }
}
