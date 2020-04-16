package edu.anadolu.ltr;

import com.google.common.collect.Sets;
import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.field.MetaTag;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.MatrixCalculator;
import edu.cmu.lti.ws4j.util.StopWordRemover;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.StringUtils;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.anadolu.cmdline.CmdLineTool.execution;
import static edu.anadolu.field.MetaTag.notEmpty;


public class DocFeatureBase {

    Document jDoc;
    String docId, url;
    String rawHTML;
    CollectionStatistics collectionStatistics;
    Tag analyzerTag;
    List<String> listContent;
    List<String> title;
    List<String> keyword;
    List<String> description;
    List<String> hTags;
    IndexSearcher searcher;
    IndexReader reader;
    RelatednessCalculator rc1;
    Map<String,Integer> mapTf;

    /**
     * It is possible to find URL info in the headers of *.warc files for the ClueWeb datasets.
     * Since Gov2 doesn't have URL info, we rely on JSoup's baseUri() method.
     *
     * @param warcRecord input warc record
     */
    DocFeatureBase(WarcRecord warcRecord, CollectionStatistics collectionStatistics, Tag analyzerTag, IndexSearcher searcher, IndexReader reader, RelatednessCalculator rc1) {
        try {
            rawHTML = warcRecord.content();
            jDoc = Jsoup.parse(rawHTML);
            docId = warcRecord.id();
            url = warcRecord.url() == null ? jDoc.baseUri() : warcRecord.url();
            this.collectionStatistics = collectionStatistics;
            this.analyzerTag = analyzerTag;
            this.listContent = Analyzers.getAnalyzedTokens(jDoc.text(), Analyzers.analyzer(analyzerTag));
            mapTf = getDocTfForTerms();
            this.searcher = searcher;
            this.reader = reader;

            this.title = Analyzers.getAnalyzedTokens(jDoc.title(),Analyzers.analyzer(analyzerTag));
            this.keyword = Analyzers.getAnalyzedTokens(MetaTag.enrich3(jDoc, "keywords"), Analyzers.analyzer(analyzerTag));
            this.description = Analyzers.getAnalyzedTokens(MetaTag.enrich3(jDoc, "description"), Analyzers.analyzer(analyzerTag));
            Elements hTags = jDoc.select("h1, h2, h3, h4, h5, h6");
            this.hTags=hTags.stream()
                    .map(e -> e.text())
                    .map(String::trim)
                    .filter(notEmpty).collect(Collectors.toList());
            this.rc1=rc1;
        } catch (Exception exception) {
            System.err.println("jdoc exception " + warcRecord.id());
            exception.printStackTrace();
            jDoc = null;
        }
    }


    String calculate(List<IDocFeature> featureList) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append(docId);
//        long start2=0;
        for (IDocFeature iDoc : featureList) {
//            start2 = System.nanoTime();
            double value = iDoc.calculate(this);
            builder.append("\t").append(iDoc.toString()).append(":").append(String.format("%.5f", value));
//            if((System.nanoTime()-start2)>1000000000)
//                System.out.println(iDoc.getClass().getSimpleName() + " "+ (System.nanoTime()-start2)/1000000000);
        }
        return builder.toString();
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
    protected Map<String, String> parseFields(SolrClient solr) {

        Map<String, String> fields = new HashMap<>();
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

    protected int inlinkCount(Document jDoc, Elements links) {
        int inlink = 0;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String domain = host.startsWith("www.") ? host.substring(4) : host;

            for (Element element : links) {
                try {
                    String href = element.attr("href");
                    if (href.startsWith("/")) {
                        inlink++;
                        continue;
                    }
                    URI urilink = new URI(href);
                    String linkhost = urilink.getHost();
                    String linkdomain = linkhost.startsWith("www.") ? linkhost.substring(4) : linkhost;
                    if (domain.equals(linkdomain)) inlink++;
                } catch (Exception e) {
                    continue;
                }
            }

        } catch (URISyntaxException e) {
            System.out.println("url syntax: " + url);
            return 0;
        } catch (NullPointerException e1){
            System.out.println("null url : " + url);
            return 0;
        }
        return inlink;
    }

    protected double textSimilarity(List<String> str1, List<String> str2) {

        String[] words1 = str1.toArray(new String[0]);
        String[] words2 = str2.toArray(new String[0]);

        if (words1.length==0) return 0;
        if (words2.length==0) return 0;
//        Pattern UNWANTED_SYMBOLS = Pattern.compile("\\p{Punct}");
//        Matcher unwantedMatcher = UNWANTED_SYMBOLS.matcher(str1);
//        str1 = unwantedMatcher.replaceAll("");
//        Matcher unwantedMatcher2 = UNWANTED_SYMBOLS.matcher(str2);
//        str2 = unwantedMatcher2.replaceAll("");
//        String[] words1 = str1.split("\\s+");
//        String[] words2 = str2.split("\\s+");

        long avgDoclen = (long)collectionStatistics.sumTotalTermFreq()/collectionStatistics.docCount();
        words1 = words1.length > avgDoclen ? copyArrayRandom(words1,avgDoclen) : words1;
        words2 = words2.length > avgDoclen ? copyArrayRandom(words2,avgDoclen) : words2;

        words1 = StopWordRemover.getInstance().removeStopWords(words1);
        words2 = StopWordRemover.getInstance().removeStopWords(words2);

        double[][] s1 = MatrixCalculator.getNormalizedSimilarityMatrix(words1, words2, rc1);
//        double[][] s1 = MatrixCalculator.getNormalizedSimilarityMatrix(words1, words2, new WuPalmer(new NictWordNet()));
        double total = 0;
        int count = 0;

        for (int i = 0; i < words1.length; i++) {
            for (int j = 0; j < words2.length; j++) {
                if (s1[i][j] > 0){
                    count++;
                    total += s1[i][j];
                }
            }
        }
        if (count == 0) return 0;
        return total / count;
    }

    protected double cosSim(String str1, String str2){
        if(str1.length()==0 || str2.length()==0) return 0;
        double score =0.0;
        try{
            score = 1-(new CosineDistance().apply(str1,str2));
        }catch (IllegalArgumentException ex){
            System.out.println("************************************************");
            System.out.println("str1 " + str1);
            System.out.println("str2 " + str2);
            System.out.println("************************************************");
        }
        return score;
    }

    private String[] copyArrayRandom(String[] array, long length){

        Random rand = new Random();
        Set<Integer> randNums = new HashSet<>();
        while(randNums.size()<length){
            randNums.add(rand.nextInt(array.length));
        }

        String[] arrayNew = new String[(int)length];
        for(int i=0;i<length;i++){
            arrayNew[i] = array[randNums.iterator().next()];
        }
        return arrayNew;
    }

    protected double getTf(String word, List<String> listContent) {
        return Collections.frequency(listContent, word);
    }


    protected String getFirstWords(String text, int wordCount) {

        if (StringUtils.isEmpty(text)) return text;

        String updatedText = null;

        String[] tokens = text.split("\\s+");
        if (tokens.length > wordCount)
            updatedText = String.join(" ", Arrays.copyOfRange(text.split("\\s+"), 0, wordCount));
        else
            updatedText = String.join(" ", tokens);
        return updatedText;
    }

    protected Map<String,Integer> getDocTfForTerms(){
        Map<String,Integer> mapTf = new HashMap<>();

        for(String term : listContent){
            if(mapTf.containsKey(term))
                mapTf.put(term,mapTf.get(term)+1);
            else
                mapTf.put(term,1);
        }
        return mapTf;
    }
}
