package edu.anadolu;


import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SelectionMethods {
public static String CONTENT_FIELD = "contents";

    private static class TermTFDF{
        private int indexID;
        private long TF;
        private long DF;

        public TermTFDF(int indexID) {
            this.indexID = indexID;
        }

        public int getIndexID() {
            return indexID;
        }

        public long getTF() {
            return TF;
        }

        public void setTF(long TF) {
            this.TF = TF;
        }

        public long getDF() {
            return DF;
        }

        public void setDF(long DF) {
            this.DF = DF;
        }
    }

    /**
     * This method returns one of the given IndexSearchers due to the changing of the most specific term in term frequency
     * If the most specific term changes in term frequency, the method returns stemmed IndexSearcher
     * @param infoNeed
     * @param searchers
     * @return
     */
    public static Searcher MSTTermFreq(InfoNeed infoNeed,Searcher[] searchers ,Similarity similarity) throws IOException {
        if(searchers.length!=2) throw new RuntimeException("We do not support selective stemming for searchers whose count is not equal to 2 yet!");

        HashMap<Tag,IndexSearcher> indexSearchers = new HashMap<>();
        HashMap<Tag,Searcher> tagSearcher = new HashMap<>();
        for(Searcher s: searchers){
            IndexSearcher is =  new IndexSearcher(s.reader);
            is.setSimilarity(similarity);
            indexSearchers.put(s.analyzerTag, is);
            tagSearcher.put(s.analyzerTag,s);
        }


        HashMap<Tag, List<String>> tokenizedTopic4searchers = new HashMap<>();
        for(Tag tag: indexSearchers.keySet())
            tokenizedTopic4searchers.put(tag,tokenize(infoNeed.query(), Analyzers.analyzer(tag)));

        HashMap<Tag, List<TermTFDF>> termTFDF4searchers = new HashMap<>();

        for(Tag tag: tokenizedTopic4searchers.keySet()){
            List<String> tokenizedTopic = tokenizedTopic4searchers.get(tag);
            List<TermTFDF> listTermTFDF = new ArrayList<>();
            IndexSearcher is = indexSearchers.get(tag);
            for(String word: tokenizedTopic){
                Term t = new Term(CONTENT_FIELD,word);
                TermStatistics termStatistics = is.termStatistics(t, TermContext.build(is.getTopReaderContext(), t));
                final long termFrequency = termStatistics.totalTermFreq();
                TermTFDF termTFDF = new TermTFDF(tokenizedTopic.indexOf(word));
                termTFDF.setTF(termFrequency);
                listTermTFDF.add(termTFDF);
            }
            listTermTFDF.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
            termTFDF4searchers.put(tag,listTermTFDF);
        }


        Tag baseTag = searchers[0].analyzerTag;
        IndexSearcher selectedIndexSearcher=indexSearchers.get(baseTag);
        List<TermTFDF> baseListTermTFDF=termTFDF4searchers.get(baseTag);
        Searcher selectedSearcher = tagSearcher.get(baseTag);

        for(Tag tag: termTFDF4searchers.keySet()){
            if(tag.compareTo(baseTag)==0) continue;
            List<TermTFDF> listTermTFDF = termTFDF4searchers.get(tag);

            if (listTermTFDF.get(0).getIndexID() != baseListTermTFDF.get(0).getIndexID()) {
                //Stem -> MST has changed
                selectedIndexSearcher = indexSearchers.get(tag);
                selectedSearcher=tagSearcher.get(tag);
                break;
            }else{
                //NoStem
            }
        }

        return selectedSearcher;
    }

    private static List<String> tokenize(String text, Analyzer analyzer){
        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(CONTENT_FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                list.add(termAtt.toString());

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }
}
