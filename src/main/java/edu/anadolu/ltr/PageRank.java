package edu.anadolu.ltr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;

import static org.apache.solr.common.params.CommonParams.*;

public class PageRank extends SolrAwareFeatureBase {

    public PageRank(HttpSolrClient solr) {
        super(solr);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {

        SolrQuery query = new SolrQuery(base.docId).setFields("rank");
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
            //TODO PageRank scores are not calculated for every document. Use default value or average for missing documents.
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
            return 96176607.1109954 / 503903810d;
        }

        if (resp.size() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.size() + " many hits!");
        }

        double rank = 0;
        try {
            rank = (double) resp.get(0).getFieldValue("rank");
        } catch (Exception ex) {
            throw new RuntimeException((base.docId + "  -  " + rank), ex);
        }


        resp.clear();
        query.clear();

        return rank;
    }
}
