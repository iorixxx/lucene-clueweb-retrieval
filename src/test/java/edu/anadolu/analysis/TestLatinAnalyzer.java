package edu.anadolu.analysis;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;

/**
 * Latin analyzer keeps words written in Latin alphabet, discards the rest.
 */
public class TestLatinAnalyzer extends BaseTokenStreamTestCase {

    /**
     * Ensure that only latin words are survived.
     *
     * @throws Exception if any
     */
    public void testLatinWords() throws Exception {

        assertAnalyzesTo(
                Analyzers.analyzer(Tag.Latin),
                "321\n" +
                        "Je t'aime \n" +
                        "Te quiero \n" +
                        "אני אוהבת אותך" +  //Hebrew
                        "\n Ich liebe dich \n" +
                        "ผมรักคุณ \n" + // Thai
                        "მიყვარხარ \n" + // Georgian
                        "seni seviyorum 123\n",

                new String[]{"321", "je", "t'aime", "te", "quiero", "ich", "liebe", "dich", "seni", "seviyorum", "123"});
    }
}
