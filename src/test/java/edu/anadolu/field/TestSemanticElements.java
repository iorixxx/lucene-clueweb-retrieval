package edu.anadolu.field;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Test for HTML5 Semantic Elements
 */
public class TestSemanticElements {

    final String doctypeHTML5 ="<!DOCTYPE html>\n";

    final String doctypeHTML401Transitional ="<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n";

    final String doctypeHTML401Frameset ="<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">\n";

    final String doctypeXHTML10Strict ="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";

    final String doctypeXHTML10Transitional ="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";

    final String doctypeXHTML10Frameset ="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">\n";

    final String doctypeXHTML11 ="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n";

    final String doctypeMathML20DTD ="<!DOCTYPE math PUBLIC \"-//W3C//DTD MathML 2.0//EN\"\t\n" +
            "\t\"http://www.w3.org/Math/DTD/mathml2/mathml2.dtd\">\n";

    final String doctypeXHTMLMathMLSVGDTD ="<!DOCTYPE html PUBLIC\n" +
            "    \"-//W3C//DTD XHTML 1.1 plus MathML 2.0 plus SVG 1.1//EN\"\n" +
            "    \"http://www.w3.org/2002/04/xhtml-math-svg/xhtml-math-svg.dtd\">\n";

    final String MathML101DTD ="<!DOCTYPE math SYSTEM \n" +
            "\t\"http://www.w3.org/Math/DTD/mathml1/mathml.dtd\">\n";

    final String html = "<abbr title=\"Internationalization\">I18N</abbr> \n"+
            "The <abbr title=\"World Health Organization\">WHO</abbr> was founded in 1948.\n"+
            "<p>\n" +
            "         <abbr title = \"Pr\">pvt.</abbr>\n" +
            "         <br />\n" +
            "         \n" +
            "         <abbr >ICC.</abbr> \n" +
            "            promotes the global game.\n" +
            "         <br />\n" +
            "      </p>"+
            "Can I get this <acronym title=\"as soon as possible\">ASAP</acronym>?"+
            "<p>The <acronym >WWW</acronym> is only a component of the Internet.</p>"+
            "<p><dfn title=\"HyperText Markup Language\">HTML</dfn> is the standard markup language for creating web pages.</p>"+
            "<p><dfn><abbr title=\"Anadolu University\">AU</abbr></dfn> is the university.</p>"+
            "<p><dfn>TV</dfn> is the television</p>"+
            "<p><cite>The Scream</cite> by Edward Munch. Painted in 1893.</p>"+
            "<p>More information can be found in <cite>[ISO-0000]</cite>.</p>"+
            "<p>Do not forget to buy <mark>daily milk</mark> today.</p>";

    @Test
    public void testSemanticStats(){
        Document jDoc = Jsoup.parse(html);
        String elements1 = SemanticElements.acronymSemantic(jDoc);
        String elements2 = SemanticElements.abbrSemantic(jDoc);
        String elements3 = SemanticElements.markSemantic(jDoc);
        String elements4 = SemanticElements.citeSemantic(jDoc);
        String elements5 = SemanticElements.dfnSemantic(jDoc);
        SemanticStats.getSemanticObject().printSemanticStats();
    }

    @Test
    public void testAcronymSemantic(){
        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.acronymSemantic(jDoc);
        Assert.assertEquals("ASAP", elements);
    }

    @Test
    public void testAbbrSemantic(){
        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.abbrSemantic(jDoc);
        Assert.assertEquals("I18N WHO AU", elements);
    }

    @Test
    public void testMarkSemantic(){
        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.markSemantic(jDoc);
        Assert.assertEquals("daily_milk", elements);
    }

    @Test
    public void testCiteSemantic(){
        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.citeSemantic(jDoc);
        Assert.assertEquals("The_Scream [ISO-0000]", elements);
    }

    @Test
    public void testDfnSemantic(){
        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.dfnSemantic(jDoc);
        Assert.assertEquals("HTML", elements);
    }

    @Test
    public void testDocType() {
        String type = "";
        Document jDoc = Jsoup.parse(doctypeXHTMLMathMLSVGDTD);
        Node typeNode = jDoc.childNode(0);
        String name = typeNode.attr("name");
        String publicId = typeNode.attr("publicId");
        String systemId = typeNode.attr("systemId");
        String[] tokens = publicId.split("//");

        if(publicId=="" && systemId.endsWith("mathml.dtd") && name.equalsIgnoreCase("math")) {
            type="MathML 1.01 - DTD";
            System.out.println(type);
            return;
        }

        if(publicId=="" && systemId=="" && name.equalsIgnoreCase("html")) {
            type="html5";
            System.out.println(type);
            return;
        }
        for (String token : tokens) {
            if (token.startsWith("DTD")){
                type=token;
                break;
            }
        }
        System.out.println(type);

    }


        @Test
    public void testArticleHeader() {

        final String html
                = "<article>\n" +
                "  <header>\n" +
                "    <h1>What Does WWF Do?</h1>\n" +
                "    <p>WWF's mission:</ p>\n" +
                "  </header>\n" +
                "  <p>WWF's mission is to stop the degradation of our planet's natural environment,\n" +
                "  and build a future in which humans live in harmony with nature.</p>\n" +
                "</article>";

        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.semanticElements(jDoc);
        Assert.assertEquals("article header", elements);
    }

    @Test
    public void testFigureCaption() {

        final String html
                = "<figure>\n" +
                "  <img src=\"pic_mountain.jpg\" alt=\"The Pulpit Rock\" width=\"304\" height=\"228\">\n" +
                "  <figcaption>Fig1. - The Pulpit Rock, Norway.</figcaption>\n" +
                "</figure>";

        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.semanticElements(jDoc);
        Assert.assertEquals("figcaption figure", elements);
    }

    @Test
    public void test() {

        final String html
                = "<body>\n" +
                "\n" +
                "<header>\n" +
                "<h1>Monday Times</h1>\n" +
                "</header>\n" +
                "\n" +
                "<nav>\n" +
                "<ul>\n" +
                "<li>News</li>\n" +
                "<li>Sports</li>\n" +
                "<li>Weather</li>\n" +
                "</ul>\n" +
                "</nav>\n" +
                "\n" +
                "<section>\n" +
                "<h2>News Section</h2>\n" +
                "<article>\n" +
                "<h2>News Article</h2>\n" +
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque in porta lorem. Morbi condimentum est nibh, et consectetur tortor feugiat at.</p>\n" +
                "</article>\n" +
                "<article>\n" +
                "<h2>News Article</h2>\n" +
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque in porta lorem. Morbi condimentum est nibh, et consectetur tortor feugiat at.</p>\n" +
                "</article>\n" +
                "</section>\n" +
                "\n" +
                "<footer>\n" +
                "<p>&copy; 2014 Monday Times. All rights reserved.</p>\n" +
                "</footer>";

        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.semanticElements(jDoc);
        Assert.assertEquals("article article footer header nav section", elements);
    }

    @Test
    public void testArticleInArticle() {

        final String html
                = "<article>\n" +
                "\n" +
                "<h2>Famous Cities</h2>\n" +
                "\n" +
                "<article>\n" +
                "<h2>London</h2>\n" +
                "<p>London is the capital city of England. It is the most populous city in the United Kingdom,\n" +
                "with a metropolitan area of over 13 million inhabitants.</p>\n" +
                "</article>\n" +
                "\n" +
                "<article>\n" +
                "<h2>Paris</h2>\n" +
                "<p>Paris is the capital and most populous city of France.</p>\n" +
                "</article>\n" +
                "\n" +
                "<article>\n" +
                "<h2>Tokyo</h2>\n" +
                "<p>Tokyo is the capital of Japan, the center of the Greater Tokyo Area,\n" +
                "and the most populous metropolitan area in the world.</p>\n" +
                "</article>\n" +
                "\n" +
                "</article>";

        Document jDoc = Jsoup.parse(html);
        String elements = SemanticElements.semanticElements(jDoc);
        Assert.assertEquals("article article article article", elements);
    }

    @Test
    public void testComparable1() throws Exception {
        final Pair<String, String> pair1 = Pair.of("A", "D");
        final Pair<String, String> pair2 = Pair.of("B", "C");
        final Pair<String, String> pair3 = Pair.of("A", "E");
        final Pair<String, String> pair4 = Pair.of("F", "C");

        TreeSet<Pair> pairSet = new TreeSet<>();
        pairSet.add(pair1);
        pairSet.add(pair2);
        pairSet.add(pair3);
        pairSet.add(pair4);

        Assert.assertFalse(pairSet.contains(Pair.of("A", "X")));
        Assert.assertFalse(pairSet.contains(Pair.of("X", "C")));
        Assert.assertTrue(pairSet.contains(Pair.of("A", "E")));
        Assert.assertFalse(pairSet.contains(Pair.of("a", "e")));

    }

    @Test
    public void testNormalize() throws Exception {
        Analyzer analyzer = MetaTag.whitespaceAnalyzer();
        TokenStream stream = analyzer.tokenStream(null, "Abc CDE  \t  Ads23. www dd-22-sD");
        stream.reset();
        while (stream.incrementToken()) {
            System.out.println(stream.getAttribute(CharTermAttribute.class).toString());
        }
    }
    @Test
    public void testTreeSet(){
        StringBuilder builder = new StringBuilder();
        TreeSet<Pair> textTitleMapper = new TreeSet<>();
        textTitleMapper.add(Pair.of("Hello","World"));
        textTitleMapper.add(Pair.of("Hello3","World3"));
        textTitleMapper.add(Pair.of("Hello2","World2"));

        for(Pair<String,String> p: textTitleMapper){
            builder.append(p.getLeft()+": "+p.getRight()+System.lineSeparator());
        }
        System.out.println(builder.toString());


    }
}
