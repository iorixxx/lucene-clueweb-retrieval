package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class StopCover implements IDocFeature {

    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        String text = base.jDoc.text();

        Analyzer analyzer = Analyzers.analyzer(base.analyzerTag);

        Set<String> stop = new HashSet<>();

        try (TokenStream ts = analyzer.tokenStream(Analyzers.FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(termAtt))
                    stop.add(termAtt.toString());
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }

        if((double) stop.size() / EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.size()>1.0){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " StopCover : " + (double) stop.size() / EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.size());
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }

        return (double) stop.size() / EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.size();
    }
}

