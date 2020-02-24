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
            URI uri = new URI(base.url);
            String host = uri.getHost();
            String domain = host.startsWith("www.") ? host.substring(4) : host;
            for (String token : base.keyword) {
                if (domain.contains(token))
                    return 1;
            }
            return 0;
        } catch (URISyntaxException | NullPointerException  e) {
            //TODO think how to handle
            return 0;
//            throw new RuntimeException("URL error for : "+base.url);
        }
    }
}

