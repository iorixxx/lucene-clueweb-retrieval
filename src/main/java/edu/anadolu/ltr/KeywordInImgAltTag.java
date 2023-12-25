package edu.anadolu.ltr;

import com.google.common.base.Strings;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
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
            Elements images = base.jDoc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
            if (images.size() == 0 || base.keyword.size() == 0) return 0;
            for (String token : base.keyword) {
                for (Element image : images) {
                    if (Strings.isNullOrEmpty(image.attr("alt"))) continue;
                    if (image.attr("alt").contains(token)) return 1;
                }
            }
            return 0;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}
