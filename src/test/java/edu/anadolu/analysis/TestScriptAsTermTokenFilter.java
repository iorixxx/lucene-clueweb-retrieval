package edu.anadolu.analysis;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

/**
 * Tests {@link ScriptAsTermTokenFilterFactory}
 */
public class TestScriptAsTermTokenFilter extends BaseTokenStreamTestCase {


    public void testScriptWithStandardTokenizer() throws IOException {
        StringReader reader = new StringReader(
                "321\n" +
                        "Je t'aime \n" +
                        "Te quiero \n" +
                        "אני אוהבת אותך" +  //Hebrew
                        "\n Ich liebe dich \n" +
                        "ผมรักคุณ \n" + // Thai
                        "მიყვარხარ \n" + // Georgian
                        "seni seviyorum 123\n");

        final StandardTokenizer input = new StandardTokenizer(newAttributeFactory());
        input.setReader(reader);
        TokenStream stream = new ScriptAsTermTokenFilterFactory(new HashMap<>()).create(input);
        assertTokenStreamContents(stream, new String[]{
                "Number",
                "Common", "Common",
                "Common", "Common",
                "Common", "Common", "Common",
                "Common", "Common", "Common",
                "Common",
                "Common",
                "Common", "Common", "Number"});
    }

    public void testScriptAsTermFilter() throws IOException {
        StringReader reader = new StringReader(
                "321\n" +
                        "Je t'aime \n" +
                        "Te quiero \n" +
                        "אני אוהבת אותך" +  //Hebrew
                        "\n Ich liebe dich \n" +
                        "ผมรักคุณ \n" + // Thai
                        "მიყვარხარ \n" + // Georgian
                        "seni seviyorum 123\n");

        final ICUTokenizer input = new ICUTokenizer();
        input.setReader(reader);
        TokenStream stream = new ScriptAsTermTokenFilterFactory(new HashMap<>()).create(input);
        assertTokenStreamContents(stream, new String[]{
                "Number",
                "Latin", "Latin",
                "Latin", "Latin",
                "Hebrew", "Hebrew", "Hebrew",
                "Latin", "Latin", "Latin",
                "Thai", "Thai", "Thai",
                "Georgian",
                "Latin", "Latin", "Number"

        });
    }

    public void testScriptWithWhiteSpaceTokenizer() throws IOException {
        StringReader reader = new StringReader(
                "321\n" +
                        "Je t'aime \n" +
                        "Te quiero \n" +
                        "אני אוהבת אותך" +  //Hebrew
                        "\n Ich liebe dich \n" +
                        "ผมรักคุณ \n" + // Thai
                        "მიყვარხარ \n" + // Georgian
                        "seni seviyorum 123\n");

        TokenStream stream = new ScriptAsTermTokenFilterFactory(new HashMap<>()).create(whitespaceMockTokenizer(reader));
        assertTokenStreamContents(stream, new String[]{
                "Common",
                "Common", "Common",
                "Common", "Common",
                "Common", "Common", "Common",
                "Common", "Common", "Common",
                "Common",
                "Common",
                "Common", "Common", "Common"});
    }

}
