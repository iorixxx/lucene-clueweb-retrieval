package edu.anadolu.ltr;

import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;

import static org.apache.solr.common.params.CommonParams.HEADER_ECHO_PARAMS;
import static org.apache.solr.common.params.CommonParams.OMIT_HEADER;

public class SpamScore extends SolrAwareFeatureBase {

    public SpamScore(Collection collection) {
        super(collection);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        SolrQuery query = new SolrQuery(base.docId).setFields("percentile");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);

        SolrDocumentList resp;
        try {
            resp = solrClient.query(query).getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }


        if (resp.size() == 0) {
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
        }

        if (resp.size() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.size() + " many hits!");
        }

        int percentile = (int) resp.get(0).getFieldValue("percentile");

        resp.clear();
        query.clear();

        if (percentile >= 0 && percentile < 100)
            return (double)percentile/100;
        else throw new RuntimeException("percentile invalid " + percentile);
    }
}
