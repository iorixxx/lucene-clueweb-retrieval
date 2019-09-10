package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.field.MetaTag;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.MatrixCalculator;
import edu.cmu.lti.ws4j.util.StopWordRemover;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.StringUtils;
import org.clueweb09.InfoNeed;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QDFeatureBase {

    Document jDoc;
    String docId, url;
    String rawHTML;
    InfoNeed query;

    /**
     * It is possible to find URL info in the headers of *.warc files for the ClueWeb datasets.
     * Since Gov2 doesn't have URL info, we rely on JSoup's baseUri() method.
     *
     * @param warcRecord input warc record
     */
    QDFeatureBase(InfoNeed query, WarcRecord warcRecord) {
        try {
            rawHTML = warcRecord.content();
            jDoc = Jsoup.parse(rawHTML);
            docId = warcRecord.id();
            url = warcRecord.url() == null ? jDoc.baseUri() : warcRecord.url();
            this.query = query;
        } catch (Exception exception) {
            System.err.println("jdoc exception " + warcRecord.id());
            jDoc = null;
        }
    }


    String calculate(List<IQDFeature> featureList) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append(query.id());
        builder.append("\t");
        builder.append(docId);
        for (IQDFeature iqd : featureList) {
            double value = iqd.calculate(this);
            builder.append("\t").append(iqd.toString()).append(":").append(String.format("%.5f", value));
        }
        return builder.toString();
    }
}