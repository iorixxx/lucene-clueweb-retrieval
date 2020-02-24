package edu.anadolu.ltr;


import edu.anadolu.Indexer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;

import java.io.IOException;
import java.util.Map;

public class CDD implements IDocFeature {

    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        // Formula is borrowed from the paper : Zhou, Y., & Croft, W. B. (2005, October). Document quality models for web ad hoc retrieval.
        double cdd = 0.0;
        double lambda = 0.8;


        
        int contentsize = base.listContent.size();
        long totalTerm = base.collectionStatistics.sumTotalTermFreq();
        for(Map.Entry<String,Integer> word : base.mapTf.entrySet()){
            Term term = new Term(Indexer.FIELD_CONTENTS, word.getKey());
            double pColl = (double) TermContext.build(base.reader.getContext(), term).totalTermFreq()/totalTerm;
            double pDoc = (double) word.getValue()/ contentsize;
            double pwd = (lambda * pDoc) + ((1-lambda) * pColl);
            cdd += pColl * Math.log(pColl/pwd);
        }

//        if(cdd>10 || cdd<-10){
//            System.out.println("****************************************************************************************************************************************");
//            System.out.println("Doc Id = " + base.docId + " CDD : " + cdd);
//            System.out.println("********************************************************************");
//            System.out.println(base.jDoc.html());
//            System.out.println("****************************************************************************************************************************************");
//        }

        return cdd;
    }
}

