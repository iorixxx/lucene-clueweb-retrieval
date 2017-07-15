package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

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
}
