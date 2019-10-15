package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class FracTableText implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        int docLength = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(Tag.NoStem)).size();

        String tableText = "";
        Elements tables = base.jDoc.select("table");

        for(Element table : tables){
            Elements rows = table.select("tr");
            for(Element row : rows){
                Elements cols = row.select("td");
                for(Element col : cols){
                    tableText+=col.text();
                }
            }
        }

        return ((double)(Analyzers.getAnalyzedTokens(tableText, Analyzers.analyzer(Tag.NoStem)).size())/docLength);
    }

}
