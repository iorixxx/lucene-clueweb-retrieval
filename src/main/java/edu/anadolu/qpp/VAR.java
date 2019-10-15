package edu.anadolu.qpp;

import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Variability Score (VAR)
 */
public class VAR extends Base {

    public VAR (Path indexPath, String field) throws IOException {
        super(indexPath, field);
    }

    @Override
    public double value (String word) {
        throw new UnsupportedOperationException();
    }

    /**
     * The implementation is borrowed from the paper:
     * Zhao, Y., Scholer, F., & Tsegay, Y. (2008). Effective pre-retrieval query performance prediction using similarity and variability evidence.
     *
     * @return Variability score
     * @throws IOException
     */
    public double value (InfoNeed need) throws IOException {

        double varScore = 0.0;
        double wdt, fdt;

        List<String> terms = getAnalyzedTokens(need.query(), analyzer);
        int validTerms = terms.size();

        for (String term : terms) {
            double wdtSum = 0.0, wdtSquareSum = 0.0;
            double variance;

            if (df(field, term) == 0) {
                System.out.println("Term " + term + " is missing in vocabulary");
                validTerms--;
                continue;
            }

            Term t = new Term(field, term);
            PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(searcher.getIndexReader(), field, t.bytes());

            if (postingsEnum == null) {
                System.out.println("Cannot find the term " + term + " in the field " + field);
                continue;
            }

            while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                fdt = postingsEnum.freq();
                wdt = 1 + Math.log(fdt) * Math.log(1 + docCount / df(field, term));
                wdtSum += wdt;
                wdtSquareSum += Math.pow(wdt, 2);
            }
            // simplified version of variance
            variance = wdtSquareSum - ((Math.pow(wdtSum, 2)) / df(field, term));
            varScore += Math.sqrt((1.0 / df(field, term)) * variance);
        }

        // normalize score by the number of valid query terms
        return varScore / validTerms;
    }

    public String toString() {
        return "VAR";
    }
}
