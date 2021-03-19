package edu.anadolu.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tr.Zemberek3StemFilterFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.anadolu.analysis.Tag.*;

/**
 * Utility to hold {@link Analyzer} implementation used in this work.
 */
public class Analyzers {

    public static final String FIELD = "field";

    public static final String[] scripts = new String[]{
            "Jpan",
            "Cyrillic",
            "Greek",
            "Arabic",
            "Hangul",
            "Thai",
            "Armenian",
            "Devanagari",
            "Hebrew",
            "Georgian"
    };

    /**
     * Intended to use with one term queries (otq) only
     *
     * @param text input string to analyze
     * @return analyzed input
     */
    public static String getAnalyzedToken(String text, Analyzer analyzer) {
        final List<String> list = getAnalyzedTokens(text, analyzer);
        if (list.size() != 1)
            throw new RuntimeException("Text : " + text + " contains more than one tokens : " + list.toString());
        return list.get(0);
    }

    /**
     * Modified from : http://lucene.apache.org/core/4_10_2/core/org/apache/lucene/analysis/package-summary.html
     */
    public static List<String> getAnalyzedTokens(String text, Analyzer analyzer) {

        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

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
                        .addTokenFilter("englishpossessive")
                        .addTokenFilter("kstem")
                        .build();

            case Snowball:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("englishpossessive")
                        .addTokenFilter("snowballporter", "language", "English")
                        .build();

            case Hunspell:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("englishpossessive")
                        .addTokenFilter("hunspellstem", "dictionary", "en_US.dic", "affix", "en_US.aff", "ignoreCase", "true")
                        .build();


            case ICU:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case Latin:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", "Latin")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case ASCII:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(BasicLatinTokenFilterFactory.class)
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", "ASCII")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case Zemberek:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter(Zemberek3StemFilterFactory.class, "strategy", "maxLength")
                        .build();

            case SnowballTurkish:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter("snowballporter", "language", "Turkish")
                        .build();

            case HunspellTurkish:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter("hunspellstem", "dictionary", "tr_TR.dic", "affix", "tr_TR.aff", "ignoreCase", "true", "longestOnly","true")
                        .build();

            case F5:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter("truncate", "prefixLength", "5")
                        .build();

            case NoStemTurkish:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .build();

            case KStemField: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();
                analyzerPerField.put("url", new SimpleAnalyzer());

                return new PerFieldAnalyzerWrapper(
                        Analyzers.analyzer(KStem), analyzerPerField);
            }

            case UAX: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();

                analyzerPerField.put("url", CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.URL])
                        .build());

                analyzerPerField.put("email", CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.EMAIL])
                        .build());

                return new PerFieldAnalyzerWrapper(CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .build(), analyzerPerField);
            }


            case Script: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();

                for (String script : scripts)
                    analyzerPerField.put(script, script(script));

                analyzerPerField.put("ascii", CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(NonASCIITokenFilterFactory.class)
                        .build());

                analyzerPerField.put("latin", CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(NonLatinTokenFilterFactory.class)
                        .build());

                return new PerFieldAnalyzerWrapper(
                        CustomAnalyzer.builder()
                                .withTokenizer("icu")
                                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                                .addTokenFilter(BasicLatinTokenFilterFactory.class)
                                .addTokenFilter(TypeAsTermTokenFilterFactory.class)
                                .build(),
                        analyzerPerField);
            }
            default:
                throw new AssertionError(Analyzers.class);

        }
    }


    private static Analyzer script(String script) throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("icu")
                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", script)
                .addTokenFilter("lowercase")
                .build();
    }

    public static void main(String[] args) {
        String text = "I'd ip1188 love your's yours' you're O'Reilly's don't eagle's feathers, or in one month's time Bernadette's, flower's, glass's, one's divers' snorkels'";
        System.out.println(getAnalyzedTokens(text, analyzer(Snowball)));
    }
}
