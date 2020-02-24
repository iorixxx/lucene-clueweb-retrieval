package edu.anadolu.ltr;

import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;
import java.util.Locale;

import static org.apache.solr.common.params.CommonParams.*;

public class NumberOfChildPages extends SolrAwareFeatureBase {

    NumberOfChildPages(Collection collection) {
        super(collection);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {

        SolrQuery query = new SolrQuery(base.docId).setFields("url").setRows(1);
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

        if (resp.getNumFound() == 0) {
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
            throw new RuntimeException("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());
        }

        if (resp.getNumFound() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.getNumFound() + " many hits!");
        }

        try {
            url = (String) resp.get(0).getFieldValue("url");
        } catch (Exception ex) {
            throw new RuntimeException((base.docId + "  -  " + base.url), ex);
        }


        resp.clear();
        query.clear();

        query = new SolrQuery("{!prefix f=url}" + url.trim().toLowerCase(Locale.ENGLISH)).setFields("id").setRows(1);
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        query.set(DF, "url");

        int count=0;
        try {
            resp = solrClient.query(query).getResults();
            count = (int) resp.getNumFound();
        }catch (SolrServerException e) {
            throw new RuntimeException(e);
        }catch (StackOverflowError e1){
            e1.printStackTrace();
            return 0;
        }catch (HttpSolrClient.RemoteSolrException e2){
            e2.printStackTrace();
            return 0;
        }


        if (count < 1) {
            throw new RuntimeException("url " + url + " docID " + base.docId + " returned " + resp.getNumFound() + " many hits!");
        }
        
        resp.clear();
        query.clear();

        return (double) count - 1;
    }
}
