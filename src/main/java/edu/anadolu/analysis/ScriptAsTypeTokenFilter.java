package edu.anadolu.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.tokenattributes.ScriptAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

public class ScriptAsTypeTokenFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final ScriptAttribute scriptAtt = addAttribute(ScriptAttribute.class);

    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    public ScriptAsTypeTokenFilter(TokenStream input) {
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

