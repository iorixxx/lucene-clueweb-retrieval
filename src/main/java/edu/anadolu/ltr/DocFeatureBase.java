package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.datasets.Collection;
import edu.anadolu.field.MetaTag;
import org.apache.solr.client.solrj.SolrClient;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class DocFeatureBase {

    private final WarcRecord warcRecord;
    Document jDoc;
    String docId, url;

    DocFeatureBase(WarcRecord warcRecord) {
        this.warcRecord = warcRecord;
        try {
            jDoc = Jsoup.parse(warcRecord.content());
            docId = warcRecord.id();
            url = warcRecord.url();
        } catch (Exception exception) {
            System.err.println("jdoc exception " + warcRecord.id());
            jDoc = null;
        }
    }


    synchronized void calculate(List<IDocFeature> featureList) {
        System.out.print(docId);
        for (IDocFeature iDoc : featureList) {
            double value = iDoc.calculate(warcRecord);
            System.out.print("\t" + iDoc.toString() + ":" + value);
        }
        System.out.println();
    }

    /**
     * Different document representations (keywords, body, title, description, URL)
     */
    protected void parseFields(Collection collection, SolrClient solr) {


        String title = null;
        String body = null;


        // HTML <title> Tag
        Element titleEl = jDoc.getElementsByTag("title").first();
        if (titleEl != null) {
            title = StringUtil.normaliseWhitespace(titleEl.text()).trim();
        }

        String keywords = MetaTag.enrich2(jDoc, "keywords");
        String description = MetaTag.enrich2(jDoc, "description");


        // HTML <body> Tag
        Element bodyEl = jDoc.body();
        if (bodyEl != null) {
            body = bodyEl.text();
        }


        String anchor = Indexer.anchor(docId, solr);

        /*
         * Try to get useful parts of the URL
         * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
         */

        final String URLString = Collection.GOV2.equals(collection) ? jDoc.baseUri() : url;

        if (URLString != null && URLString.length() > 5) {

            String url;

            try {

                final URL aURL = new URL(URLString);

                url = (aURL.getHost() + " " + aURL.getFile()).trim();

                if (aURL.getRef() != null)
                    url += " " + aURL.getRef();

                String host = aURL.getHost();

            } catch (MalformedURLException me) {
                System.out.println("Malformed URL = " + URLString);
            }

        }
    }
}
