package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;

import java.util.List;

public class AvgTermLength implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        List<String> terms = Analyzers.getAnalyzedTokens(base.jDoc.text(), Analyzers.analyzer(base.analyzerTag));
        if(terms.size()==0) return 0;

        if(terms.stream().mapToInt(w -> w.length()).average().getAsDouble()<3){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " AvgTermCount : " + terms.stream().mapToInt(w -> w.length()).average().getAsDouble());
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }


        return terms.stream().mapToInt(w -> w.length()).average().getAsDouble();
    }
}
