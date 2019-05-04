package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;

public class StopWordRatio implements IDocFeature {

    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        String text = base.jDoc.text();

        Analyzer analyzer = Analyzers.analyzer(Tag.NoStem);

        int docLength = 0;
        int stopCount = 0;

        try (TokenStream ts = analyzer.tokenStream(Analyzers.FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                docLength++;
                if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(termAtt))
                    stopCount++;
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }

        if (docLength == 0) return -1.0;

        return (double) stopCount / docLength;
    }
}
