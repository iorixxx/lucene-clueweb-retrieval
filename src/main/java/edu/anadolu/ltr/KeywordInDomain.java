package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.field.MetaTag;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class KeywordInDomain implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String keyword = MetaTag.enrich3(base.jDoc, "keywords");
            URI uri = new URI(base.url);
            String host = uri.getHost();
            String domain = host.startsWith("www.") ? host.substring(4) : host;
            String[] keywords = keyword.split(",");
            for (String token : keywords) {
                token = Analyzers.getAnalyzedToken(token, MetaTag.whitespaceAnalyzer());
                if (domain.contains(token))
                    return 1;
            }
            return 0;
        } catch (URISyntaxException | NullPointerException | IOException e) {
            //return 0;
            throw new RuntimeException("URL error for : "+base.url);
        }
    }
}

