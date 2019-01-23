package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
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
            for (String token : Analyzers.getAnalyzedTokens(keyword, Analyzers.analyzer(Tag.NoStem))) {
                if (domain.contains(token))
                    return 1;
            }
            return 0;
        } catch (URISyntaxException | NullPointerException  e) {
            //return 0;
            throw new RuntimeException("URL error for : "+base.url);
        }
    }
}

