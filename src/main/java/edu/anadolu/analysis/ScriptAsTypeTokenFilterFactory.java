package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.Map;


public class ScriptAsTypeTokenFilterFactory extends TokenFilterFactory {
    /**
     * Creates a new ScriptAsTypeTokenFilterFactory
     */

    public ScriptAsTypeTokenFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new ScriptAsTypeTokenFilter(input);
    }

    /**
     * Delegates script attribute to the type attribute.
     * Inspired/modified from {@link org.apache.lucene.analysis.payloads.TypeAsPayloadTokenFilter}.
     * It removes tokens whose types appear in a set of blocked scripts when combined with a {@link org.apache.lucene.analysis.core.TypeTokenFilter}.
     */
    class ScriptAsTypeTokenFilter extends TokenFilter {

        private final ScriptAttribute scriptAtt = addAttribute(ScriptAttribute.class);
        private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

        ScriptAsTypeTokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public final boolean incrementToken() throws IOException {
            if (input.incrementToken()) {

                String script = scriptAtt.getName();

                if (script != null && !script.isEmpty()) {
                    typeAtt.setType(script);
                }

                return true;
            } else {
                return false;
            }
        }
    }
}


