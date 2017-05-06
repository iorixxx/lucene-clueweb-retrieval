package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

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
}


