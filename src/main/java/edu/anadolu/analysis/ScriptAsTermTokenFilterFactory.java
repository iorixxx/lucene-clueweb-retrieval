package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Factory for {@link ScriptAsTermTokenFilter}.
 */

public class ScriptAsTermTokenFilterFactory extends TokenFilterFactory {

    /**
     * Creates a new ScriptAsTermTokenFilterFactory
     */
    public ScriptAsTermTokenFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new ScriptAsTermTokenFilter(input);
    }


    /**
     * Sets term attribute as the script type
     */
    public class ScriptAsTermTokenFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final ScriptAttribute scriptAtt = addAttribute(ScriptAttribute.class);

        private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

        ScriptAsTermTokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public final boolean incrementToken() throws IOException {

            if (input.incrementToken()) {

                String type = typeAtt.type();

                if ("<NUM>".equals(type)) {
                    termAtt.setEmpty().append("Number");
                    return true;
                }

                String script = scriptAtt.getName();

                if (script != null && !script.isEmpty()) {
                    termAtt.setEmpty().append(script);
                } else
                    termAtt.setEmpty().append("NULL");

                return true;
            } else {
                return false;
            }
        }
    }
}
