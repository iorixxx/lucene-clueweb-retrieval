package edu.anadolu.qpp;

import edu.anadolu.analysis.Analyzers;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.similarities.ModelBase;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;

import static org.apache.lucene.search.similarities.ModelBase.log2;

/**
 * The PMI of a pair of outcomes x and y belonging to discrete random variables X and Y quantifies the discrepancy between
 * the probability of their coincidence given their joint distribution and their individual distributions, assuming independence.
 */
public class PMI extends Base {

    private final QueryParser queryParser;

    public PMI(Path indexPath) throws IOException {
        super(indexPath, "contents");
        queryParser = new QueryParser(field, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);
    }


    /**
     * the number of documents containing at both of the terms
     */
    private int term1ANDterm2(String term1, String term2) throws IOException, ParseException {
        return searcher.count(queryParser.parse(term1 + " " + term2));
    }

    @Override
    public double value(String word) {
        throw new UnsupportedOperationException();
    }


    public long analyzedDF(String field, String word) throws IOException {
        return df(field, Analyzers.getAnalyzedToken(word, analyzer));
    }

    public double pmi(String m1, String m2) throws IOException, ParseException {
        return log2(docCount * (double) term1ANDterm2(m1, m2) / (double) (analyzedDF(field, m1) * analyzedDF(field, m2)));
    }


    public double value(InfoNeed need) throws IOException, ParseException {

        double pmi = 0.0;
        int counter = 0;

        String[] distinctTerms = need.distinctSet.toArray(new String[0]);

        if (distinctTerms.length == 1) {
            // TODO what is the value of average PMI for one term query?
            return 0.0;
        }

        for (int i = 0; i < distinctTerms.length; i++) {
            final String m1 = distinctTerms[i];
            for (int j = i + 1; j < distinctTerms.length; j++) {
                final String m2 = distinctTerms[j];

                pmi += ModelBase.log2((double) term1ANDterm2(m1, m2) / (double) (analyzedDF(field, m1) * analyzedDF(field, m2)));
                counter++;

            }
        }
        return pmi / counter;
    }
}

