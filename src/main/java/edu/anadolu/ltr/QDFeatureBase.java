package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.clueweb09.InfoNeed;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
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


    /**
     * It is possible to find URL info in the headers of *.warc files for the ClueWeb datasets.
     * Since Gov2 doesn't have URL info, we rely on JSoup's baseUri() method.
     *
     * @param warcRecord input warc record
     */
    QDFeatureBase(InfoNeed query, WarcRecord warcRecord, CollectionStatistics collectionStatistics, Map<String, TermStatistics> termStatisticsMap, Tag analyzerTag) {
        try {
            rawHTML = warcRecord.content();
            jDoc = Jsoup.parse(rawHTML);
            docId = warcRecord.id();
            url = warcRecord.url() == null ? jDoc.baseUri() : warcRecord.url();
            this.query = query;
            this.collectionStatistics = collectionStatistics;
            this.termStatisticsMap = termStatisticsMap;
            this.analyzerTag = analyzerTag;

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
            double score = 0.0;
            for (String word : subParts) {
                tf = getTf(word,listContent);
                score += iqd.calculate(this, word, subParts);
            }
            builder.append("\t").append(iqd.toString()).append(":").append(String.format("%.5f", score));


        }
        return builder.toString();
    }

    protected double getTf(String word, List<String> listContent) {
        return Collections.frequency(listContent, word);
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