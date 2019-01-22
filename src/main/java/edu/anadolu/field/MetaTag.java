package edu.anadolu.field;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * HTML <meta> Tag Helper
 * <a href="https://www.w3schools.com/TAgs/tag_meta.asp">HTML meta tag</a>
 */
public class MetaTag {

    public static final Predicate<String> notEmpty = (String s) -> s != null && !s.isEmpty();


    /**
     * <head>
     * <meta charset="UTF-8">
     * <meta name="description" content="Free Web tutorials">
     * <meta name="keywords" content="HTML,CSS,XML,JavaScript">
     * <meta name="author" content="John Doe">
     * <meta name="viewport" content="width=device-width, initial-scale=1.0">
     * </head>
     *
     * @param jDoc JSoup document
     * @return Name attributes of the meta tags
     */
    public static String metaTagsWithNameAttribute(Document jDoc) {

        Elements elements = jDoc.head().select("meta[name~=\\S+]");

        if (elements.isEmpty()) return null;

        StringBuilder builder = new StringBuilder();

        elements.stream()
                .map(e -> e.attr("name"))
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        elements.empty();
        return builder.toString().trim();
    }

    /**
     * Enrich Lucene document with metadata extracted from JSoup document.
     * <p>
     * <head>
     * <meta charset="UTF-8">
     * <meta name="description" content="Free Web tutorials">
     * <meta name="keywords" content="HTML,CSS,XML,JavaScript">
     * <meta name="author" content="Hege Refsnes">
     * </head>
     *
     * @param jDoc JSoup document
     * @param meta name attribute of the meta tag
     */
    public static String enrich(org.jsoup.nodes.Document jDoc, String meta) {

        Elements elements = jDoc.head().select("meta[name=" + meta + "]");
        elements.addAll(jDoc.select("meta[name=" + meta + "s]"));

        if (elements.isEmpty()) return null;

        StringBuilder builder = new StringBuilder();

        elements.stream()
                .map(e -> e.attr("content"))
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        elements.stream()
                .map(e -> e.attr("contents"))
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        elements.empty();
        return builder.toString().trim();
    }

    /**
     * Enrich Lucene document with metadata extracted from JSoup document.
     * <p>
     * <head>
     * <meta charset="UTF-8">
     * <meta name="description" content="Free Web tutorials">
     * <meta name="keywords" content="HTML,CSS,XML,JavaScript">
     * <meta name="author" content="Hege Refsnes">
     * </head>
     *
     * @param jDoc JSoup document
     * @param meta name attribute of the meta tag
     */
    public static String enrich2(org.jsoup.nodes.Document jDoc, String meta) {

        Elements elements = jDoc.head().select("meta[name=" + meta + "]");

        if (elements.isEmpty()) return null;

        StringBuilder builder = new StringBuilder();

        elements.stream()
                .map(e -> e.attr("content"))
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(' '));

        elements.empty();
        return builder.toString().trim();
    }

    /**
     * Enrich Lucene document with metadata plus open graph metadata extracted from JSoup document.
     * <p>
     * <head>
     * <meta charset="UTF-8">
     * <meta name="description" content="Free Web tutorials">
     * <meta name="keywords" content="HTML,CSS,XML,JavaScript">
     * <meta name="author" content="Hege Refsnes">
     * </head>
     *
     * @param jDoc JSoup document
     * @param meta name attribute of the meta tag
     */
    public static String enrich3(org.jsoup.nodes.Document jDoc, String meta) {

        Elements elements = jDoc.head().select("meta[name=" + meta + "]");
        elements.addAll(jDoc.head().select("meta[property=og:" + meta + "]"));

        if (elements.isEmpty()) return null;

        StringBuilder builder = new StringBuilder();

        elements.stream()
                .map(e -> e.attr("content"))
                .map(String::trim)
                .filter(notEmpty)
                .forEach(s -> builder.append(s).append(", "));

        elements.empty();
        return builder.toString().trim();
    }

    public static Analyzer whitespaceAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("lowercase")
                .build();
    }
}
