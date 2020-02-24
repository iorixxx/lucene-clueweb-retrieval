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
        int textlen = base.jDoc.text().replaceAll("\\s+","").length();
        if(textlen==0) return 0;

        int tablelen = 0;
        Elements tds = base.jDoc.select("body > table > tbody > tr > td");
        for(Element td : tds){
            tablelen += td.text().replaceAll("\\s+","").length();
        }

        if((double)(tablelen)/textlen>1.0){
            System.out.println("********************************************************************");
            System.out.println("Doc Id = " + base.docId + " Table Size : " + tablelen + " Doc Len : " + textlen);
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("********************************************************************");
        }

        return (double)(tablelen)/textlen;
    }

}
