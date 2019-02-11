package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.Map;

public class TypeAsTermTokenFilterFactory extends TokenFilterFactory {

    /**
     * Creates a new TypeAsTermTokenFilterFactory
     */
    public TypeAsTermTokenFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public TokenFilter create(TokenStream input) {
        return new TypeAsTermTokenFilter(input);
    }

    private final class TypeAsTermTokenFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

        private TypeAsTermTokenFilter(TokenStream input) {
            super(input);
        }

        @Override
        public final boolean incrementToken() throws IOException {

            if (input.incrementToken()) {

                String type = typeAtt.type();

                if (type != null && !type.isEmpty()) {
                    termAtt.setEmpty().append(type);
                } else
                    termAtt.setEmpty().append("NULL");

                return true;
            } else {
                return false;
            }
        }
    }
}
