package edu.anadolu.ltr;


import edu.anadolu.Indexer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.TermStatistics;

import java.io.IOException;

public class CDD implements IDocFeature {

    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) throws IOException {
        // Formula is borrowed from the paper : Zhou, Y., & Croft, W. B. (2005, October). Document quality models for web ad hoc retrieval.
        double cdd = 0.0;
        double lambda = 0.8;

        for(String word : base.listContent){
            Term term = new Term(Indexer.FIELD_CONTENTS, word);
            TermStatistics termStatistics = base.searcher.termStatistics(term, TermContext.build(base.reader.getContext(), term));
            double pColl = (double) termStatistics.totalTermFreq()/base.collectionStatistics.sumTotalTermFreq();
            double pDoc = base.getTf(word,base.listContent)/base.listContent.size();
            double pwd = (lambda * pDoc) + ((1-lambda) * pColl);
            cdd += pColl * Math.log(pColl/pwd);
        }

        return cdd;
    }
}

