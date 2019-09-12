package edu.anadolu.ltr;

import edu.anadolu.datasets.Collection;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;

import java.io.IOException;

import static org.apache.solr.common.params.CommonParams.*;

public class PageRank extends SolrAwareFeatureBase {

    public PageRank(Collection collection) {
        super(collection);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {

        SolrQuery query = new SolrQuery(base.docId).setFields("rank").setRows(1);
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        query.set(DF, "id");

        SolrDocumentList resp;
        try {
            resp = solrClient.query(query).getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }


        if (resp.getNumFound() == 0) {
            //TODO PageRank scores are not calculated for every document. Use the default minimum pagerank value or the average for missing documents.
            System.out.println("cannot find docID " + base.docId + " in " + solrClient.getBaseURL());

            if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {

                /*
                 *  "stats":{
                 *     "stats_fields":{
                 *       "rank":{
                 *         "min":0.15,
                 *         "max":39694.290336,
                 *         "count":502505807,
                 *         "missing":0,
                 *         "sum":9.615165129285386E7,
                 *         "sumOfSquares":3.769931471784484E9,
                 *         "mean":0.19134435851972922,
                 *         "stddev":2.732334503888498}}}}
                 */

                return 0.15; // 96176607.1109954 / 503903810d;

            } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection) || Collection.NTCIR.equals(collection)) {

                /*
                 *   "stats":{
                 *     "stats_fields":{
                 *       "rank":{
                 *         "min":-22.2287221840378,
                 *         "max":-15.2896180719556,
                 *         "count":733019372,
                 *         "missing":0,
                 *         "sum":-1.6198355689383139E10,
                 *         "sumOfSquares":3.580651128059826E11,
                 *         "mean":-22.098127700482024,
                 *         "stddev":0.3905033835067088}}}}
                 */

                return -22.2287221840378;

            } else throw new RuntimeException("PageRank is not available for " + collection);

        }

        if (resp.getNumFound() != 1) {
            System.out.println("docID " + base.docId + " returned " + resp.getNumFound() + " many hits!");
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
