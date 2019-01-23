package edu.anadolu.ltr;

import edu.anadolu.field.MetaTag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class KeywordInImgAltTag implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String keyword = MetaTag.enrich3(base.jDoc, "keywords");
            Elements images = base.jDoc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
            if (images.size() == 0 || keyword.length() == 0) return 0;
            String[] keywords = keyword.split(",");
            for (String token : keywords) {
                for (Element image : images) {
                    if (image.attr("alt") == null) continue;
                    if (image.attr("alt").equals("")) continue;
                    if (image.attr("alt").contains(token)) return 1;
                }
            }
            return 0;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}
