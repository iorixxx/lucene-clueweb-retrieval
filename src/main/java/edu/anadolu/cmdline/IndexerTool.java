package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.exp.ROB04;
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

    @Option(name = "-tag", metaVar = "[KStem|NoStem|ICU]", required = false, usage = "Analyzer Tag")
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

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;

        if (Collection.MQ09.equals(collection) || Collection.MQE1.equals(collection)) {
            System.out.println("No need to run separate indexer for MQ09!");
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


        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));

        if (docsPath == null || indexPath == null) {
            System.out.println(getHelp());
            return;
        }

        if (anchor) {

            final HttpSolrClient solr;
            if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE1.equals(collection)) {
                solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor09A").build();
            } else if (Collection.CW12B.equals(collection))
                solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor12A").build();
            else {
                System.out.println("anchor text is only available to ClueWeb09 and ClueWeb12 collections!");
                return;
            }

            long s = System.nanoTime();
            Indexer i = new Indexer(collection, docsPath, indexPath, solr, anchor, tag);
            int nIndexed = i.indexWithThreads(numThreads);
            solr.close();
            System.out.println("Total " + nIndexed + " documents (with anchor text) indexed in " + execution(s));

        } else {

            long start = System.nanoTime();
            Indexer indexer = new Indexer(collection, docsPath, indexPath, null, anchor, tag);
            int numIndexed = indexer.indexWithThreads(numThreads);
            System.out.println("Total " + numIndexed + " documents (without anchor text) indexed in " + execution(start));

        }
    }
}
