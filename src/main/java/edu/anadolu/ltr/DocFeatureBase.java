package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.field.MetaTag;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.client.solrj.SolrClient;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocFeatureBase {

    private final WarcRecord warcRecord;
    Document jDoc;
    String docId, url;

    Analyzer analyzer = Analyzers.analyzer(Tag.NoStem);

    /**
     * Clueweb collection comes with a warcRecord which has url info in header
     * Since Gov2 doesn't have that info jsoup provides that info by baseUri()
     * @param warcRecord
     */
    DocFeatureBase(WarcRecord warcRecord) {
        this.warcRecord = warcRecord;
        try {
            jDoc = Jsoup.parse(warcRecord.content());
            docId = warcRecord.id();
            url = warcRecord.url() == null ? jDoc.baseUri() : warcRecord.url();
        } catch (Exception exception) {
            System.err.println("jdoc exception " + warcRecord.id());
            jDoc = null;
        }
    }


    synchronized void calculate(List<IDocFeature> featureList) throws IOException{
        System.out.print(docId);
        for (IDocFeature iDoc : featureList) {
            double value = iDoc.calculate(this);
            System.out.print("\t" + iDoc.toString() + ":" + value);
        }
        System.out.println();
    }

    /**
     * Returns meta element
     */
    protected Element getMetaElement(String attrName) {
        Element element = jDoc.select("meta[name=" + attrName + "]").first();
        Element elementog = jDoc.select("meta[property=og:" + attrName + "]").first();

        if (element != null) return element;
        else if (elementog != null) return elementog;
        return null;
    }

    /**
     * Different document representations (keywords, body, title, description, URL)
     */
    protected Map<String,String> parseFields(SolrClient solr) {

        Map<String,String> fields = new HashMap<>();
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

        final String URLString = this.url;
        String host=null;
        if (URLString != null && URLString.length() > 5) {

            String url;

            try {

                final URL aURL = new URL(URLString);

                url = (aURL.getHost() + " " + aURL.getFile()).trim();

                if (aURL.getRef() != null)
                    url += " " + aURL.getRef();

                 host = aURL.getHost();

            } catch (MalformedURLException me) {
                System.out.println("Malformed URL = " + URLString);
            }

        }

        fields.put("title",title);
        fields.put("keywords",keywords);
        fields.put("description",description);
        fields.put("url",url);
        fields.put("host",host);
        fields.put("body",body);
        fields.put("anchor",anchor);

        return fields;
    }

    protected int inlinkCount(Document jDoc, Elements links){
        int inlink=0;
        try {
            URI uri = uri = new URI(jDoc.baseUri());
            String host = uri.getHost();
            String domain = host.startsWith("www.") ? host.substring(4) : host;

            for(Element element : links){
                try{
                    String href = element.attr("href");
                    if(href.startsWith("/")){
                        inlink++;
                        continue;
                    }
                    URI urilink=new URI(href);
                    String linkhost = urilink.getHost();
                    String linkdomain = linkhost.startsWith("www.") ? linkhost.substring(4) : linkhost;
                    if(domain.equals(linkdomain)) inlink++;
                }catch (Exception e){
                    continue;
                }
            }

        } catch (URISyntaxException | NullPointerException e) {
            return 0;
        }
        return inlink;
    }
}
