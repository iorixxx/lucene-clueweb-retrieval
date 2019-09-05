package edu.anadolu.ltr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;

import static org.apache.solr.common.params.CommonParams.*;

public class NumberOfChildPages extends SolrAwareFeatureBase {

    NumberOfChildPages(HttpSolrClient solr) {
        super(solr);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {

        SolrQuery query = new SolrQuery(base.docId).setFields("url");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        query.set(DF, "id");

        SolrDocumentList resp;
        try {
            resp = solrClient.query(query).getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }

        String url;

        if (resp.size() == 0) {
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
            url = base.url;
        }

        if (resp.size() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.size() + " many hits!");
        }

        try {
            url = (String) resp.get(0).getFieldValue("url");
        } catch (Exception ex) {
            throw new RuntimeException((base.docId + "  -  " + base.url), ex);
        }


        resp.clear();
        query.clear();

        query = new SolrQuery("{!prefix f=url}" + url.trim()).setFields("id");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        query.set(DF, "url");


        try {
            resp = solrClient.query(query).getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }


        if (resp.size() < 1) {
            throw new RuntimeException("url " + url + " docID " + base.docId + " returned " + resp.size() + " many hits!");
        }

        query.clear();

        return (double) resp.getNumFound() - 1;
    }
}
