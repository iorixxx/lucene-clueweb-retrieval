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

        int textlen = base.jDoc.text().replaceAll("\\s+","").length();
        if(textlen==0) return 0;
        Elements links = base.jDoc.select("a[href]");
        int anchorlen = 0;

        for(Element element : links){
            if(Jsoup.parse(element.html()).select("a[href]").size()>0) continue;
            anchorlen+=element.text().replaceAll("\\s+","").length();
        }


        if(((double)(anchorlen)/textlen)>1.0){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " Anchor Size : " + anchorlen + " Doc Len : " + textlen);
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
            return 0;
        }
        
        return ((double)(anchorlen)/textlen);
    }

}
