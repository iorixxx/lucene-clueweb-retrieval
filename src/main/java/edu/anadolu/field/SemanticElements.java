package edu.anadolu.field;

import edu.anadolu.Indexer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class to extract HTML5 Semantic Elements
 * <a href="https://www.w3schools.com/html/html5_semantic_elements.asp">HTML5 Semantic Elements</a>
 */
public class SemanticElements {
    private static SemanticStats semanticStats = SemanticStats.getSemanticObject();

    private static final String[] elements = new String[]{"article", "aside", "details", "figcaption", "figure", "footer", "header", "main", "mark", "nav", "section", "summary", "time",
            //"abbr",
            // "acronym",
            // "em",
            //  "strong",
            //  "cite",
            //  "dfn"
    };

    /**
     * NOTE: This can be loaded from a file.
     */
    private static SemanticTag.HTMLTag[] selectedHTMLTag= new SemanticTag.HTMLTag[]{SemanticTag.HTMLTag.abbr,
            SemanticTag.HTMLTag.acronym, SemanticTag.HTMLTag.dfn,SemanticTag.HTMLTag.cite,SemanticTag.HTMLTag.mark};

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

        semanticStats.incDocType(docType(jDoc));

        // make a new, empty document
        Document document = new Document();
        document.add(new StringField("id", wDoc.id(), Field.Store.YES));
        document.add(new Indexer.NoPositionsTextField("tags", semanticElements(jDoc)));
        document.add(new Indexer.NoPositionsTextField("abbr", abbrSemantic(jDoc)));
        document.add(new Indexer.NoPositionsTextField("acronym", acronymSemantic(jDoc)));
        document.add(new Indexer.NoPositionsTextField("cite", citeSemantic(jDoc)));
        document.add(new Indexer.NoPositionsTextField("dfn", dfnSemantic(jDoc)));
        document.add(new Indexer.NoPositionsTextField("mark", markSemantic(jDoc)));

        return document;
    }
    static String dfnSemantic(org.jsoup.nodes.Document jDoc){
        StringBuilder dfns = new StringBuilder();
        List<SemanticTag> selectedTags = semanticElementsWithAttr(jDoc);
        boolean isFind=false;
        for(SemanticTag tag:selectedTags){
            if(tag.getTag().compareTo(SemanticTag.HTMLTag.dfn)!=0) continue;
            String title=tag.findAttr("title").getRight();
            if(title.length() < 3) continue;
            String text=tag.getText().replaceAll("\\s+","_");
            dfns.append(text).append(' ');
            if(!isFind) {
                semanticStats.incTagDF("dfn");
                isFind=true;
            }
            semanticStats.incTagTF("dfn");
            semanticStats.putTextWithTitle(title,text);
        }
        return dfns.toString().trim();
    }

    static String markSemantic(org.jsoup.nodes.Document jDoc){
        StringBuilder marks = new StringBuilder();
        List<SemanticTag> selectedTags = semanticElementsWithAttr(jDoc);
        boolean isFind=false;
        for(SemanticTag tag:selectedTags){
            if(tag.getTag().compareTo(SemanticTag.HTMLTag.mark)!=0) continue;
            String text=tag.getText().replaceAll("\\s+","_");
            marks.append(text).append(' ');
            if(!isFind) {
                semanticStats.incTagDF("mark");
                isFind=true;
            }
            semanticStats.incTagTF("mark");
        }
        return marks.toString().trim();
    }

    static String citeSemantic(org.jsoup.nodes.Document jDoc){
        StringBuilder cites = new StringBuilder();
        List<SemanticTag> selectedTags = semanticElementsWithAttr(jDoc);
        boolean isFind=false;
        for(SemanticTag tag:selectedTags){
            if(tag.getTag().compareTo(SemanticTag.HTMLTag.cite)!=0) continue;
            String text=tag.getText().replaceAll("\\s+","_");
            cites.append(text).append(' ');
            if(!isFind) {
                semanticStats.incTagDF("cite");
                isFind=true;
            }
            semanticStats.incTagTF("cite");
        }
        return cites.toString().trim();
    }

    static String acronymSemantic(org.jsoup.nodes.Document jDoc){
        StringBuilder acronyms = new StringBuilder();
        List<SemanticTag> selectedTags = semanticElementsWithAttr(jDoc);
        boolean isFind=false;
        for(SemanticTag tag:selectedTags){
            if(tag.getTag().compareTo(SemanticTag.HTMLTag.acronym)!= 0) continue;
            String title=tag.findAttr("title").getRight();
            if(title.length() < 3) continue;
            String text=tag.getText().replaceAll("\\s+","_");
            acronyms.append(text).append(' ');
            if(!isFind) {
                semanticStats.incTagDF("acronym");
                isFind=true;
            }
            semanticStats.incTagTF("acronym");
            semanticStats.putTextWithTitle(title, text);
        }
        return acronyms.toString().trim();
    }

    static String abbrSemantic(org.jsoup.nodes.Document jDoc){
        StringBuilder abbrs = new StringBuilder();
        List<SemanticTag> selectedTags = semanticElementsWithAttr(jDoc);
        boolean isFind=false;
        for(SemanticTag tag:selectedTags){
            if(tag.getTag().compareTo(SemanticTag.HTMLTag.abbr)!=0) continue;
            String title=tag.findAttr("title").getRight();
            if(title.length() < 3) continue;
            String text=tag.getText().replaceAll("\\s+","_");
            abbrs.append(text).append(' ');
            if(!isFind) {
                semanticStats.incTagDF("abbr");
                isFind=true;
            }
            semanticStats.incTagTF("abbr");
            semanticStats.putTextWithTitle(title, text);
        }
        return abbrs.toString().trim();

    }

    static String docType(org.jsoup.nodes.Document jDoc){
        String type = "";
        Node typeNode = jDoc.childNode(0);
        String name = typeNode.attr("name");
        String publicId = typeNode.attr("publicId");
        String systemId = typeNode.attr("systemId");
        String[] tokens = publicId.split("//");

        if(publicId=="" && systemId=="" && name.equalsIgnoreCase("html")) {
            type="html5";
            return type;
        }

        if(publicId=="" && systemId.endsWith("mathml.dtd") && name.equalsIgnoreCase("math")) {
            type="MathML 1.01 - DTD";
            return type;
        }

        for (String token : tokens) {
            if (token.startsWith("DTD")){
                type=token;
                return type;
            }
        }
        return "not detected";
    }

    @Deprecated
    static String semanticElements(org.jsoup.nodes.Document jDoc) {

        StringBuilder tags = new StringBuilder();

        for (String tag : elements) {

            Elements elements = jDoc.getElementsByTag(tag);

            for (int i = 0; i < elements.size(); i++)
                tags.append(tag).append(' ');
        }

        return tags.toString().trim();
    }

    /**
     * Return list of tags with their text value <tag,text>
     * @param jDoc
     * @return
     */
    static List<SemanticTag> semanticElementsWithAttr(org.jsoup.nodes.Document jDoc) {
        List<SemanticTag> semanticTags = new LinkedList<>();
        for (SemanticTag.HTMLTag tag : selectedHTMLTag) {

            Elements elements = jDoc.getElementsByTag(tag.toString());

            for (Element e:elements){
                SemanticTag st = new SemanticTag(tag,e.text());
                for (Attribute attr:e.attributes()){
                    st.addAttr(Pair.of(attr.getKey(),attr.getValue()));
                }
                semanticTags.add(st);
            }
        }
        return semanticTags;
    }
}
