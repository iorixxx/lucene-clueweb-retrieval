package edu.anadolu.ltr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;

import static org.apache.solr.common.params.CommonParams.*;

public class InLinkCount extends SolrAwareFeatureBase {

    InLinkCount(HttpSolrClient solr) {
        super(solr);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {

        SolrQuery query = new SolrQuery(base.docId).setFields("count");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        query.set(DF, "id");

        SolrDocumentList resp;
        try {
            resp = solrClient.query(query).getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }


        if (resp.size() == 0) {
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
            return 0.0;
        }

        if (resp.size() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.size() + " many hits!");
        }

        int count = 0;
        try {
            count = (int) resp.get(0).getFieldValue("count");
        } catch (Exception ex) {
            throw new RuntimeException((base.docId + "  -  " + count), ex);
        }


        resp.clear();
        query.clear();

        return (double) count;
    }
}