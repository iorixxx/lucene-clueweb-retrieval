package edu.anadolu.ltr;

import org.apache.lucene.search.similarities.ModelBase;

import java.util.*;

public class Entropy implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        if (base.listContent.size()==0) return 0;

        double entropy = 0.0;
        int contentSize = base.listContent.size();

        for (Map.Entry<String,Integer> term : base.mapTf.entrySet()) {

            double p = (double) term.getValue() / contentSize;

            entropy += p * -ModelBase.log2(p);

        }

//        if(entropy<0 || entropy>10){
//            System.out.println("****************************************************************************************************************************************");
//            System.out.println("Doc Id = " + base.docId + " Entropy : " + entropy);
//            System.out.println("********************************************************************");
//            System.out.println(base.jDoc.html());
//            System.out.println("****************************************************************************************************************************************");
//        }

        return entropy;
    }
}
