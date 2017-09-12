package edu.anadolu.field;

import edu.anadolu.Indexer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.Arrays;

/**
 * Helper class to extract HTML5 Semantic Elements
 * <a href="https://www.w3schools.com/html/html5_semantic_elements.asp">HTML5 Semantic Elements</a>
 */
public class SemanticElements {

    private static final String[] elements = new String[]{"article", "aside", "details", "figcaption", "figure", "footer", "header", "main", "mark", "nav", "section", "summary", "time",
            "abbr",
            "acronym",
            "em",
            "strong",
            "cite",
            "dfn"};

    static {
        Arrays.sort(elements);
    }

    /**
     * Indexes HTML5 Semantic Elements as tokens.
     *
     * @param wDoc WarcRecord
     * @return Lucene Document having semantic tags are
     */
    public static Document warc2LuceneDocument(WarcRecord wDoc) {

        org.jsoup.nodes.Document jDoc;
        try {
            jDoc = Jsoup.parse(wDoc.content());
        } catch (Exception exception) {
            System.err.println(wDoc.id());
            return null;
        }

        // make a new, empty document
        Document document = new Document();
        document.add(new StringField("id", wDoc.id(), Field.Store.YES));
        document.add(new Indexer.NoPositionsTextField("tags", semanticElements(jDoc)));

        return document;
    }

    static String semanticElements(org.jsoup.nodes.Document jDoc) {

        StringBuilder tags = new StringBuilder();

        for (String tag : elements) {

            Elements elements = jDoc.getElementsByTag(tag);

            for (int i = 0; i < elements.size(); i++)
                tags.append(tag).append(' ');
        }

        return tags.toString().trim();
    }
}
