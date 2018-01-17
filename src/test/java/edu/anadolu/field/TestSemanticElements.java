package edu.anadolu.field;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for HTML5 Semantic Elements
 */
public class TestSemanticElements {

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
