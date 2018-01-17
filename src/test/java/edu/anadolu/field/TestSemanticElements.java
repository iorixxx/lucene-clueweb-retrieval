package edu.anadolu.field;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test for HTML5 Semantic Elements
 */
public class TestSemanticElements {

    @Test
    public void testSemanticElementsWithAttr() {
        final String html = "<abbr title=\"Internationalization\">I18N</abbr> \n"+
                "The <abbr title=\"World Health Organization\">WHO</abbr> was founded in 1948.\n"+
                "<p>\n" +
                "         <abbr title = \"Private\">pvt.</abbr>\n" +
                "         <br />\n" +
                "         \n" +
                "         <abbr title = \"International Cricket Council\">ICC.</abbr> \n" +
                "            promotes the global game.\n" +
                "         <br />\n" +
                "      </p>"+
                "Can I get this <acronym title=\"as soon as possible\">ASAP</acronym>?"+
                "<p>The <acronym title=\"World Wide Web\">WWW</acronym> is only a component of the Internet.</p>"+
                "<p><dfn title=\"HyperText Markup Language\">HTML</dfn> is the standard markup language for creating web pages.</p>"+
                "<p><dfn><abbr title=\"Anadolu University\">AU</abbr></dfn> is the university.</p>"+
                "<p><dfn>TV</dfn> is the television</p>"+
                "<p><cite>The Scream</cite> by Edward Munch. Painted in 1893.</p>"+
                "<p>More information can be found in <cite>[ISO-0000]</cite>.</p>"+
                "<p>Do not forget to buy <mark>milk</mark> today.</p>";


        Document jDoc = Jsoup.parse(html);
        List<SemanticTag> elements = SemanticElements.semanticElementsWithAttr(jDoc);

        Assert.assertEquals(13, elements.size());
        for (SemanticTag st:elements) {
            if(st.getTag().toString().equals(SemanticTag.HTMLTag.abbr.toString())){
                if(st.getText().equals("I18N")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("Internationalization",st.getAttrList().get(0).getRight());
                }
                else if(st.getText().equals("WHO")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("World Health Organization",st.getAttrList().get(0).getRight());
                }
                else if(st.getText().equals("pvt.")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("Private",st.getAttrList().get(0).getRight());
                }else if(st.getText().equals("ICC.")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("International Cricket Council",st.getAttrList().get(0).getRight());
                }else if(st.getText().equals("AU")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("Anadolu University",st.getAttrList().get(0).getRight());
                }

                else Assert.fail(st.getText()+" abbr not found!");
            }
            else if(st.getTag().toString().equals(SemanticTag.HTMLTag.acronym.toString())){
                if(st.getText().equals("ASAP")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("as soon as possible",st.getAttrList().get(0).getRight());
                }
                else if(st.getText().equals("WWW")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("World Wide Web",st.getAttrList().get(0).getRight());
                }
                else Assert.fail(st.getText()+" acronym not found!");
            }
            else if(st.getTag().toString().equals(SemanticTag.HTMLTag.dfn.toString())){
                if(st.getText().equals("HTML")){
                    Assert.assertEquals("title",st.getAttrList().get(0).getLeft());
                    Assert.assertEquals("HyperText Markup Language",st.getAttrList().get(0).getRight());
                }
                else if(st.getText().equals("AU")){
                    Assert.assertEquals(0,st.getAttrList().size());
                }
                else if(st.getText().equals("TV")){
                    Assert.assertEquals(0,st.getAttrList().size());
                }
                else Assert.fail(st.getText()+" dfn not found!");
            }
            else if(st.getTag().toString().equals(SemanticTag.HTMLTag.cite.toString())){
                if(st.getText().equals("The Scream")){
                    Assert.assertEquals(0,st.getAttrList().size());
                }
                else if(st.getText().equals("[ISO-0000]")){
                    Assert.assertEquals(0,st.getAttrList().size());
                }
                else Assert.fail(st.getText()+" cite not found!");
            }
            else if(st.getTag().toString().equals(SemanticTag.HTMLTag.mark.toString())){
                if(st.getText().equals("milk")){
                    Assert.assertEquals(0,st.getAttrList().size());
                }
                else Assert.fail(st.getText()+" mark not found!");
            }
            else  Assert.fail(st.getTag()+" tag not found!");
        }
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

}
