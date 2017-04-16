package edu.anadolu.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to hold {@link Analyzer} implementation used in this work.
 */
public class Analyzers {

    private static final String FIELD = "field";

    /**
     * Filters {@link ClassicTokenizer} with {@link org.apache.lucene.analysis.standard.ClassicFilter},
     * {@link org.apache.lucene.analysis.core.LowerCaseFilter} and {@link org.apache.lucene.analysis.en.KStemFilter}.
     */
    public static Analyzer analyzer() {
        try {
            return CustomAnalyzer.builder()
                    .withTokenizer("classic")
                    .addTokenFilter("classic")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("kstem")
                    .build();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


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
        try (TokenStream ts = analyzer().tokenStream(FIELD, new StringReader(text))) {

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

    public static long getNumTerms(String text) {

        long numTerms = 0;
        try (Tokenizer ts = new ClassicTokenizer(); Reader reader = new StringReader(text)) {

            ts.setReader(reader);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                numTerms++;

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return numTerms;
    }

    /**
     * Modified from org.apache.lucene.analysis.miscellaneous.FingerprintFilter
     *
     * @param text sample text
     * @return set of unique terms
     */
    public static CharArraySet getUniqueTerms(String text) {

        final CharArraySet uniqueTerms = new CharArraySet(8, false);

        try (Reader reader = new StringReader(text); TokenStream ts = Analyzers.analyzer().tokenStream(FIELD, reader)) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {

                final char term[] = termAtt.buffer();
                final int length = termAtt.length();

                if (!uniqueTerms.contains(term, 0, length)) {
                    // clone the term, and add to the set of seen terms.
                    final char[] clonedLastTerm = new char[length];
                    System.arraycopy(term, 0, clonedLastTerm, 0, length);
                    uniqueTerms.add(clonedLastTerm);
                }
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return uniqueTerms;
    }

    public static int getNumberOfUniqueTerms(String text) {
        return getUniqueTerms(text).size();
    }
}
