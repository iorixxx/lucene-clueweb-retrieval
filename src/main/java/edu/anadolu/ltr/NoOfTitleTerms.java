package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.field.MetaTag;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NoOfTitleTerms implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        if(Analyzers.getAnalyzedTokens(base.jDoc.title(), Analyzers.analyzer(base.analyzerTag)).size()>30){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " NoofTitleTerms : " + Analyzers.getAnalyzedTokens(base.jDoc.title(), Analyzers.analyzer(base.analyzerTag)).size());
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }
        return Analyzers.getAnalyzedTokens(base.jDoc.title(), Analyzers.analyzer(base.analyzerTag)).size();
    }
}
