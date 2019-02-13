package edu.anadolu.analysis;

import com.ibm.icu.lang.UScript;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.Map;

public class NonLatinTokenFilterFactory extends TokenFilterFactory {

    /**
     * Creates a new NonLatinTokenFilterFactory
     */
    public NonLatinTokenFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new NonLatinTokenFilter(input);
    }

    private final class NonLatinTokenFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        private final ScriptAttribute scriptAtt = addAttribute(ScriptAttribute.class);

        private NonLatinTokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public final boolean incrementToken() throws IOException {

            if (!input.incrementToken()) return false;

            int code = scriptAtt.getCode();

            if (code == UScript.LATIN || code == UScript.COMMON) {
                termAtt.setEmpty().append("latin");
            } else {
                termAtt.setEmpty().append("non");

            }
            return true;
        }
    }
}
