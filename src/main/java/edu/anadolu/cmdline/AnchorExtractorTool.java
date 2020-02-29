package edu.anadolu.cmdline;

import edu.anadolu.AnchorTextExtractor;
import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.kohsuke.args4j.Option;

import java.util.Properties;

/**
 * Anchor Text Extractor Tool for ClueWeb09 category B corpus
 */
final class AnchorExtractorTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Anchor Text Extractor Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.qrels freq.fields paths.indexes paths.freqs";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));


        if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection)) {


            final HttpSolrClient solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/anchor09A").build();
            final String anchorPath = props.getProperty("paths.docs.anchor.CW09");

            for (int i = 1; i <= 10; i++) {
                final long start = System.nanoTime();
                AnchorTextExtractor.extractWithThreads(anchorPath + "/ClueWeb09_English_" + i, numThreads, solr);
                solr.commit();
                System.out.println("ClueWeb09_English_" + i + ": Anchor Text extraction completed in " + execution(start));
            }

            solr.commit();
            solr.close();
        } else if (Collection.CW12B.equals(collection)) {

            final HttpSolrClient solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/anchor12A").build();

            final String anchorPath = props.getProperty("paths.docs.anchor.CW12B");

            for (int i = 1; i <= 4; i++) {
                final long start = System.nanoTime();
                AnchorTextExtractor.extractWithThreads(anchorPath + "/Disk" + i, numThreads, solr);
                solr.commit();
                System.out.println("ClueWeb12_B13_AnchorText/Disk" + i + ": Anchor Text extraction completed in " + execution(start));
            }

            solr.optimize();
            solr.close();
        }
    }
}
