package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;


public class FracAnchorText implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        int docLength = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(Tag.NoStem)).size();
        Elements links = base.jDoc.select("a");
        List<String> anchor = new ArrayList<>();

        for(Element element : links){
            anchor.addAll(Analyzers.getAnalyzedTokens(element.text(), Analyzers.analyzer(Tag.NoStem)));
        }

        return ((double)(anchor.size())/docLength);
    }

}
