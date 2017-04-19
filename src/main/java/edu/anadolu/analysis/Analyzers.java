package edu.anadolu.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static edu.anadolu.analysis.Tag.KStem;

/**
 * Utility to hold {@link Analyzer} implementation used in this work.
 */
public class Analyzers {

    private static final String FIELD = "field";

    /**
     * Intended to use with one term queries (otq) only
     *
     * @param text input string to analyze
     * @return analyzed input
     */
    public static String getAnalyzedToken(String text) {
        final List<String> list = getAnalyzedTokens(text);
        if (list.size() != 1)
            throw new RuntimeException("Text : " + text + " contains more than one tokens : " + list.toString());
        return list.get(0);
    }

    /**
     * Modified from : http://lucene.apache.org/core/4_10_2/core/org/apache/lucene/analysis/package-summary.html
     */
    public static List<String> getAnalyzedTokens(String text) {

        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer(KStem).tokenStream(FIELD, new StringReader(text))) {

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


    public static Analyzer analyzer(Tag tag) {
        try {
            return anlyzr(tag);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static Analyzer anlyzr(Tag tag) throws IOException {

        switch (tag) {

            case NoStem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .build();

            case KStem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case ICU:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter("lowercase")
                        .build();
            default:
                throw new AssertionError(Analyzers.class);

        }

    }
}
