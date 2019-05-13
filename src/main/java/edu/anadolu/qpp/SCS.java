package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Simplified Clarity Score (SCS)
 */
public class SCS extends Base {

    public SCS(Path indexPath, String field) throws IOException {
        super(indexPath, field);
    }

    @Override
    public double value(String word) {
        throw new UnsupportedOperationException();
    }

    /**
     * Kullback-Leibler between a (simplified) query language model and a collection model
     * qtf is the number of occurrences of a term in the query
     * ctf is the number of occurrences of a term in the collection
     */
    public double value(InfoNeed need) throws IOException {

        double scs = 0.0;
        double qtf, ctf;

        List<String> terms = getAnalyzedTokens(need.query(), analyzer);

        for (String term : terms) {
            qtf = (double) Collections.frequency(terms, term) / terms.size();
            ctf = (double) ctf(field, term) / sumTotalTermFreq;

            // the specificity of a query term is assumed to be large if it does not exist in the collection
            scs += qtf * Math.log(ctf == 0.0 ? Double.MAX_VALUE : qtf / ctf);
        }

        return scs;
    }

    public String toString() {
        return "SCS";
    }
}