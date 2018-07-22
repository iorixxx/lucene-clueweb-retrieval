package edu.anadolu.spam;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.SpamTool;
import edu.anadolu.datasets.DataSet;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SpamRemovingIndexer extends Indexer {

    private final HttpSolrClient solr;
    private final int spamThreshold;

    public SpamRemovingIndexer(DataSet dataset, String docsDir, String indexPath, HttpSolrClient solr, Tag tag, IndexerConfig config, int spam) throws IOException {
        super(dataset, docsDir, indexPath, null, tag, config);
        this.solr = solr;
        this.spamThreshold = spam;

        if (spam == 0) return;
        this.indexPath = Paths.get(indexPath, spam + "_" + tag);

        if (!Files.exists(this.indexPath))
            Files.createDirectories(this.indexPath);
    }

    @Override
    protected boolean skip(String docId) {
        try {
            int percentile = SpamTool.percentile(solr, docId);
            return (percentile < spamThreshold);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }
}