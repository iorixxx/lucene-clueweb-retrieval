package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
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

//        Cleaner cleaner = new Cleaner(Whitelist.basic());
//        if(!cleaner.isValid(base.jDoc)) {
//            System.out.println(base.docId + " is invalid");
//            return 0;
//        }
//        System.out.println(base.docId + " is valid");
        List<String> text = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(base.analyzerTag));
        if(text.size()==0) return 0;
        Elements links = base.jDoc.select("a[href]");
        String anchorText = "";

        for(Element element : links){
            if(Jsoup.parse(element.html()).select("a[href]").size()>0) continue;
            anchorText+=element.text() + " ";
        }

        List<String> anchor = Analyzers.getAnalyzedTokens(anchorText, Analyzers.analyzer(base.analyzerTag));

        if(anchor.size()>text.size()){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " Anchor Size : " + anchor.size() + " Doc Len : " + text.size());
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }
        
        return ((double)(anchor.size())/text.size());
    }

}
