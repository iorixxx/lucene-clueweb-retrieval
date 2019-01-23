package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class NoFollowToAll implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String robots = MetaTag.enrich3(base.jDoc, "robots");
        if (robots != null && robots.contains("nofollow")) {
            return 1;
        }

        int nofollow = 0;
        Elements links = base.jDoc.select("a");
        if (links.size() == 0) return 0;
        for (Element element : links) {
            String rel = element.attr("rel");
            if (rel.contains("nofollow")) nofollow++;
        }

        return (double) nofollow / links.size();
    }
}

