package edu.anadolu.ltr;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.field.MetaTag;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.InfoNeed;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static edu.anadolu.Indexer.FIELD_ID;


public class QDFeatureBase {

    Document jDoc;
    String docId, url;
    String rawHTML;
    InfoNeed query;
    CollectionStatistics collectionStatistics;
    Map<String,TermStatistics> termStatisticsMap;
    Tag analyzerTag;
    double qtf = 0.0;
    double tf = 0.0;
    long dl = 0;
    long uniqueDl = 0;
    List<String> listContent;
    Collection collection;
    HttpSolrClient solrClient;


    /**
     * It is possible to find URL info in the headers of *.warc files for the ClueWeb datasets.
     * Since Gov2 doesn't have URL info, we rely on JSoup's baseUri() method.
     *
     * @param warcRecord input warc record
     */
    QDFeatureBase(InfoNeed query, WarcRecord warcRecord, CollectionStatistics collectionStatistics, Map<String, TermStatistics> termStatisticsMap, Tag analyzerTag, Collection collection, HttpSolrClient solrClient) {
        try {
            rawHTML = warcRecord.content();
            jDoc = Jsoup.parse(rawHTML);
            docId = warcRecord.id();
            url = warcRecord.url() == null ? jDoc.baseUri() : warcRecord.url();
            this.query = query;
            this.collectionStatistics = collectionStatistics;
            this.termStatisticsMap = termStatisticsMap;
            this.analyzerTag = analyzerTag;
            this.collection = collection;
            this.solrClient = solrClient;

            List<String> subParts = Analyzers.getAnalyzedTokens(query.query(), Analyzers.analyzer(analyzerTag));

            listContent = Analyzers.getAnalyzedTokens(jDoc.text(), Analyzers.analyzer(analyzerTag));

            dl = listContent.size();
            uniqueDl = new HashSet<String>(listContent).size();
            for (String word : subParts) {
                qtf += getTf(word, listContent);
            }
        } catch (Exception exception) {
            System.err.println("jdoc exception at QDFeatureBase" + warcRecord.id());
            jDoc = null;
            throw new RuntimeException(exception);
        }
    }




    String calculate(List<IQDFeature> featureList) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append(query.id());
        builder.append("\t");
        builder.append(docId);
        for (IQDFeature iqd : featureList) {

            List<String> subParts = Analyzers.getAnalyzedTokens(query.query(), Analyzers.analyzer(analyzerTag));
            List<QDFeatureFields> fields = new ArrayList<>();
            Map<String, String> fieldContents = null;

            if(iqd.field() != QDFeatureFields.ALL){
                fields.add(iqd.field());
            }else{
                fields.add(QDFeatureFields.TITLE);
                fields.add(QDFeatureFields.BODY);
                fields.add(QDFeatureFields.ANCHOR);
                fields.add(QDFeatureFields.URL);
                fields.add(QDFeatureFields.WHOLE);
            }

            for(QDFeatureFields field : fields){

                if(field != QDFeatureFields.TITLE_BODY){
                    listContent.clear();
                    fieldContents = parseFields();
                    if(field == QDFeatureFields.TITLE)  listContent = Analyzers.getAnalyzedTokens(fieldContents.get("title"), Analyzers.analyzer(analyzerTag));
                    if(field == QDFeatureFields.BODY)  listContent = Analyzers.getAnalyzedTokens(fieldContents.get("body"), Analyzers.analyzer(analyzerTag));
                    if(field == QDFeatureFields.ANCHOR)  listContent = Analyzers.getAnalyzedTokens(fieldContents.get("anchor"), Analyzers.analyzer(analyzerTag));
                    if(field == QDFeatureFields.URL)    listContent.addAll(Analyzers.getAnalyzedTokens(fieldContents.get("url"), new SimpleAnalyzer()));

                    if(field == QDFeatureFields.WHOLE){
                        listContent.addAll(Analyzers.getAnalyzedTokens(fieldContents.get("url"), new SimpleAnalyzer()));
                        listContent.addAll(Analyzers.getAnalyzedTokens(fieldContents.get("title"), Analyzers.analyzer(analyzerTag)));
                        listContent.addAll(Analyzers.getAnalyzedTokens(fieldContents.get("body"), Analyzers.analyzer(analyzerTag)));
                        listContent.addAll(Analyzers.getAnalyzedTokens(fieldContents.get("anchor"), Analyzers.analyzer(analyzerTag)));
                    }
                }

                double score = 0.0;
                double[] termScore = new double[subParts.size()];
                int i=0;
                for (String word : subParts) {
                    tf = getTf(word,listContent);
                    termScore[i] = iqd.calculate(this, word, subParts);
                    i++;
                }

                String fieldName = field==QDFeatureFields.TITLE_BODY?"":field+"";
                
                if(iqd.type() == QDFeatureType.ALL){
                    builder.append("\t").append(iqd.toString()+QDFeatureType.SUM+fieldName).append(":").append(String.format("%.5f", Arrays.stream(termScore).sum()));
                    builder.append("\t").append(iqd.toString()+QDFeatureType.MIN+fieldName).append(":").append(String.format("%.5f", Arrays.stream(termScore).min().getAsDouble()));
                    builder.append("\t").append(iqd.toString()+QDFeatureType.MAX+fieldName).append(":").append(String.format("%.5f", Arrays.stream(termScore).max().getAsDouble()));
                    builder.append("\t").append(iqd.toString()+QDFeatureType.MEAN+fieldName).append(":").append(String.format("%.5f", Arrays.stream(termScore).average().getAsDouble()));
                    builder.append("\t").append(iqd.toString()+QDFeatureType.VARIANCE+fieldName).append(":").append(String.format("%.5f", StatUtils.variance(termScore)));
                    continue;
                }

                if(iqd.type() == QDFeatureType.SUM)    score = Arrays.stream(termScore).sum();
                if(iqd.type() == QDFeatureType.MIN)    score = Arrays.stream(termScore).min().getAsDouble();
                if(iqd.type() == QDFeatureType.MAX)    score = Arrays.stream(termScore).max().getAsDouble();
                if(iqd.type() == QDFeatureType.MEAN)    score = Arrays.stream(termScore).average().getAsDouble();
                if(iqd.type() == QDFeatureType.VARIANCE)  score = StatUtils.variance(termScore);
                //For minimum coverage features (exceptional case)
                if(iqd.type() == QDFeatureType.DIFF){
                    double max = Arrays.stream(termScore).max().getAsDouble();
                    double min = Arrays.stream(termScore).min().getAsDouble();
                    score = min==-1?0:(max-min);
                }

                builder.append("\t").append(iqd.toString()+iqd.type()+fieldName).append(":").append(String.format("%.5f", score));
            }

        }
        return builder.toString();
    }

    protected double getTf(String word, List<String> listContent) {
        return Collections.frequency(listContent, word);
    }


    /**
     * Different document representations (keywords, body, title, description, URL)
     */
    protected Map<String, String> parseFields() {

        Map<String, String> fields = new HashMap<>();
        String title = "";
        String body = "";


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

        String anchor = Indexer.anchor(docId, this.solrClient);
        if(anchor==null) anchor="";
        
        /*
         * Try to get useful parts of the URL
         * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
         */
        final String URLString = this.url;
        String host = null;
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

        fields.put("title", title);
        fields.put("keywords", keywords);
        fields.put("description", description);
        fields.put("url", url);
        fields.put("host", host);
        fields.put("body", body);
        fields.put("anchor", anchor);

        return fields;
    }

    /*
     * Not used since we get tf and dl on the fly
     */
    private DocTermStat getDocTermStats(String docId, String word, String field, IndexSearcher searcher) throws IOException {
        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(searcher.getIndexReader(), field, term.bytes());

        if (postingsEnum == null) {
            System.out.println("Cannot find the word " + word + " in the field " + field);
            return new DocTermStat(word, -1, -1);
        }

        while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

            final int luceneId = postingsEnum.docID();

            if( !docId.equals(searcher.doc(luceneId).get(FIELD_ID))) continue;



            NumericDocValues norms = MultiDocValues.getNormValues(searcher.getIndexReader(), field);
            if (norms.advanceExact(luceneId)) {
                return new DocTermStat(word, norms.longValue(), postingsEnum.freq());
            } else {
                throw new RuntimeException("norms.advanceExact() cannot find " + luceneId);
            }
        }

        return null;
    }

    class DocTermStat {

        private final long dl;
        private final int tf;
        private final String word;

        DocTermStat(String word, long dl, int tf) {
            this.dl = dl;
            this.tf = tf;
            this.word = word;
        }
    }
}