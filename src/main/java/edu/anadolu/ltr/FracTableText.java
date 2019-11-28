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
        int docLength = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(base.analyzerTag)).size();
        if(docLength==0) return 0;

        String tableText = "";
        Elements tds = base.jDoc.select("body > table > tbody > tr > td");
        for(Element td : tds){
            tableText += td.text() + " ";
        }

        List<String> table = Analyzers.getAnalyzedTokens(tableText, Analyzers.analyzer(base.analyzerTag));

        if((double)(table.size())/docLength>0.99){
            System.out.println("********************************************************************");
            System.out.println("Doc Id = " + base.docId + " Table Size : " + table.size() + " Doc Len : " + docLength);
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("********************************************************************");
        }

        return (double)(table.size())/docLength;
    }

}
