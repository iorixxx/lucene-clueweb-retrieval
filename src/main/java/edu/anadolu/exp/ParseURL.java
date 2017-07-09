package edu.anadolu.exp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;

/**
 * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
 */
public class ParseURL {

    public static void main(String[] args) throws Exception {

        URL aURL = new URL("http://example.com:80/docs/books/tutorial"
                + "/index.html?name=networking&key=value#DOWNLOADING");

        System.out.println("protocol = " + aURL.getProtocol());
        System.out.println("authority = " + aURL.getAuthority());
        System.out.println("host = " + aURL.getHost());
        System.out.println("port = " + aURL.getPort());
        System.out.println("path = " + aURL.getPath());
        System.out.println("query = " + aURL.getQuery());
        System.out.println("filename = " + aURL.getFile());
        System.out.println("ref = " + aURL.getRef());

        jSoupTest();
    }


    static void jSoupTest() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>Title of the document</title>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"description\" content=\"Free Web tutorials\">\n" +
                "  <meta name=\"keywords\" content=\"HTML,CSS,XML,JavaScript\">\n" +
                "  <meta name=\"author\" content=\"John Doe\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<p>All meta information goes in the head section...</p>\n" +
                "\n" +
                "</body>\n" +
                "</html>";

        Document jDoc = Jsoup.parse(html);

        System.out.printf(jDoc.text());
    }
}
