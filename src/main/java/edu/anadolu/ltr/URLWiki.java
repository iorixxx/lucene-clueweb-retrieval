package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class URLWiki implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            URI url = new URI(base.url);

            String host = url.getHost();

            return host.contains("wikipedia")?1.0:0.0;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return 0.0;
    }

}




