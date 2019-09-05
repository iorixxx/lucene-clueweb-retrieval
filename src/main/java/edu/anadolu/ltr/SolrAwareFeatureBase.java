package edu.anadolu.ltr;

import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.Closeable;
import java.io.IOException;

/**
 * Base class for features that need to be communicate to Solr instances.
 */
abstract class SolrAwareFeatureBase implements IDocFeature, Closeable {

    final HttpSolrClient solrClient;

    SolrAwareFeatureBase(HttpSolrClient solrClient) {
        this.solrClient = solrClient;
    }

    @Override
    public void close() throws IOException {
        System.out.println("close is called");
        solrClient.close();
    }
}
