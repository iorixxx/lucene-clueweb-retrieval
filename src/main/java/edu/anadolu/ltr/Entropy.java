package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.search.similarities.ModelBase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Entropy implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        if (base.jDoc.body() == null) return 0;
        String text = base.jDoc.body().text();

        List<String> listContent = Analyzers.getAnalyzedTokens(text, Analyzers.analyzer(base.analyzerTag));
        Set<String> setContent = new HashSet<>(listContent);


        double entropy = 0.0;

        for (String term : setContent) {

            int tf = Collections.frequency(listContent, term);

            assert tf < 0 : "within document frequency should be greater than zero";

            double p = (double) tf / listContent.size();

            entropy += p * -ModelBase.log2(p);

        }

        if(entropy<0 || entropy>10){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " Entropy : " + entropy);
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }

        return entropy;
    }
}
