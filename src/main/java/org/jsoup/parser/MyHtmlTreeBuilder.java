package org.jsoup.parser;

import org.jsoup.nodes.Document;

import java.io.StringReader;

/**
 * Custom {@link HtmlTreeBuilder} implementation that avoids an infinite loop
 */
public class MyHtmlTreeBuilder extends HtmlTreeBuilder {

    @Override
    public void runParser() {
        int i = 0;
        while (true) {
            Token token = tokeniser.read();
            process(token);
            token.reset();

            if (token.type == Token.TokenType.EOF)
                break;

            if (i++ == Integer.MAX_VALUE) {
                System.out.println("infinite loop");
                break;
            }
        }
    }

    public static Document parse(String html) {
        TreeBuilder treeBuilder = new MyHtmlTreeBuilder();
        return treeBuilder.parse(new StringReader(html), "", ParseErrorList.noTracking(), treeBuilder.defaultSettings());
    }
}
