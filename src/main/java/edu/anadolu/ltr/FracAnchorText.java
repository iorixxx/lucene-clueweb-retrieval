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
        List<String> text = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(base.analyzerTag));
        int docLength = text.size();
        if(docLength==0) return 0;
        Elements links = base.jDoc.select("a[href]");
        List<String> anchor = new ArrayList<>();

        for(Element element : links){
            anchor.addAll(Analyzers.getAnalyzedTokens(element.text(), Analyzers.analyzer(base.analyzerTag)));
        }

        if(anchor.size()>docLength)  {
            System.out.println("******************************************************************************************");
            for(String anc : anchor) {
                if(!text.contains(anc))
                    System.out.print(anc + " ");
            }
            System.out.println("******************************************************************************************");

            for(Element element : links){
                System.out.println(element);
            }
            System.out.println("******************************************************************************************");

            System.out.println(base.jDoc.html());
            System.out.println("******************************************************************************************");


        }

        return ((double)(anchor.size())/docLength);
    }

}
