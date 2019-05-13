package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.Map;

public class NonASCIITokenFilterFactory extends TokenFilterFactory {

    /**
     * Creates a new NonASCIITokenFilterFactory
     */
    public NonASCIITokenFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new NonASCIITokenFilter(input);
    }

    private final class NonASCIITokenFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        private NonASCIITokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public final boolean incrementToken() throws IOException {

            if (!input.incrementToken()) return false;

            char[] buffer = this.termAtt.buffer();
            int length = this.termAtt.length();

            for (int i = 0; i < length; i++) {

                final int codePoint = Character.codePointAt(buffer, i, length);

                if (codePoint < 0 || codePoint >= 128) {
                    termAtt.setEmpty().append("non");
                    return true;
                }
            }
            termAtt.setEmpty().append("ascii");
            return true;
        }
    }
}
