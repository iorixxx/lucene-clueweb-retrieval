package edu.anadolu.field;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.junit.Assert;
import org.junit.Test;

import static org.jsoup.Jsoup.parseBodyFragment;

public class MetaTest {

    private final Document jDoc;

    public MetaTest() {
        jDoc = Jsoup.parse(html);
    }

    private final String html = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>Title of the document</title>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <meta name=\"description\" content=\"Free Web tutorials\">\n" +
            "  <meta name=\"keywords\" content=\"HTML,CSS,XML,JavaScript\">\n" +
            "  <meta name=\"author\" content=\"John Doe\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <meta name=\"   \" content=\"\">\n" +
            "  <meta name=\"test\">\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "<p>All meta information goes in the head section...</p>\n" +
            "\n" +
            "</body>\n" +
            "</html>";

    @Test
    public void testMetaNames() {

        Assert.assertEquals("Title of the document All meta information goes in the head section...", jDoc.text());

        String metaNames = MetaTag.metaTagsWithNameAttribute(jDoc);

        Assert.assertEquals("description keywords author viewport test", metaNames);

        Document dirty = parseBodyFragment(html, "");
        Cleaner cleaner = new Cleaner(Whitelist.none());
        Document clean = cleaner.clean(dirty);

        String htmlStripped = clean.text();
        Assert.assertEquals("Title of the document All meta information goes in the head section...", htmlStripped);
    }

    @Test
    public void testKeywords() {
        String keywords = MetaTag.enrich2(jDoc, "keywords");
        Assert.assertEquals("HTML,CSS,XML,JavaScript", keywords);
    }

    @Test
    public void testDescription() {
        String description = MetaTag.enrich2(jDoc, "description");
        Assert.assertEquals("Free Web tutorials", description);
    }

    @Test
    public void testNullContent() {
        String test = MetaTag.enrich2(jDoc, "non");
        Assert.assertNull(test);
    }

    @Test
    public void testEmptyContent() {
        String test = MetaTag.enrich2(jDoc, "test");
        Assert.assertNotNull(test);
        Assert.assertTrue(test.isEmpty());
    }
}
