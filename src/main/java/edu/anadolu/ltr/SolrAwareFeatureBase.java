package edu.anadolu.ltr;

import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.Closeable;
import java.io.IOException;

/**
 * Base class for features that need to be communicate to Solr instances.
 */
abstract class SolrAwareFeatureBase implements IDocFeature, Closeable {

    final HttpSolrClient solrClient;
    final Collection collection;

    SolrAwareFeatureBase(Collection collection) {
        this.solrClient = SEOTool.solrClientFactory(collection, this.getClass());
        this.collection = collection;
    }

    @Override
    public void close() throws IOException {
        solrClient.close();
    }
}
