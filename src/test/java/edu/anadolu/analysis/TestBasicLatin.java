package edu.anadolu.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.junit.Test;

import java.io.IOException;

public class TestBasicLatin extends BaseTokenStreamTestCase {


    @Test
    public void tesBasicLatin() throws IOException {

        Analyzer a = CustomAnalyzer.builder()
                .withTokenizer("icu")
                .addTokenFilter("lowercase")
                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                .addTokenFilter(BasicLatinTokenFilterFactory.class)
                .build();


        assertAnalyzesTo(a, "321\n" +
                        "Je t'aime \n" +
                        "Te quiero \n" +
                        "\n Ich liebe dich \n" +
                        "ผมรักคุณ \n" + // Thai
                        "მიყვარხარ \n" + // Georgian
                        "seni çok seviyorum 123\n", // Turkish
                new String[]{"321", "je", "t'aime", "te", "quiero", "ich", "liebe", "dich", "ผม", "รัก", "คุณ", "მიყვარხარ", "seni", "çok", "seviyorum", "123"},
                new String[]{"ASCII", "ASCII", "ASCII", "ASCII", "ASCII", "ASCII", "ASCII", "ASCII", "Thai", "Thai", "Thai", "Georgian", "ASCII", "Latin", "ASCII", "ASCII"});

    }


    @Test
    public void tesBasicLatin2() throws IOException {

        Analyzer a = CustomAnalyzer.builder()
                .withTokenizer("icu")
                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                .addTokenFilter(BasicLatinTokenFilterFactory.class)
                .build();


        assertAnalyzesTo(a, "b3s Ulusal Bulut Bilişim ve Büyük Veri Sempozyumu", // Turkish
                new String[]{"b3s", "Ulusal", "Bulut", "Bilişim", "ve", "Büyük", "Veri", "Sempozyumu"},
                new String[]{"ASCII", "ASCII", "ASCII", "Latin", "ASCII", "Latin", "ASCII", "ASCII"});

    }


}
