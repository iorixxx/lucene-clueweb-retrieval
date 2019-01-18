package edu.anadolu.qpp;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Query Scope:
 * Similar to the clarity score, an alternative indication of the generality/speciality
 * of a query is the size of the document set containing at least one of the query terms.
 */
public class Scope extends Base {

    private final QueryParser queryParser;

    public Scope(Path indexPath) throws IOException {
        super(indexPath, "contents");
        queryParser = new QueryParser(field, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
    }

    /**
     * the number of documents containing at least one of the query terms
     */
    private int scope(InfoNeed need) throws IOException, ParseException {
        return searcher.count(queryParser.parse(need.query()));
    }

    @Override
    public double value(String word) {
        throw new UnsupportedOperationException();
    }

    /**
     * The implementation is borrowed from the paper:
     * He, B., & Ounis, I. (2004). A query-based pre-retrieval model selection approach to information retrieval.
     * The implementation is slightly different (divided by / Math.log(docCount)) in
     * He, B., & Ounis, I. (2004) Inferring Query Performance Using Pre-retrieval Predictors.
     *
     * @return Query scope
     * @throws IOException
     * @throws ParseException
     */
    public double value(InfoNeed need) throws IOException, ParseException {
        return -Math.log((double) scope(need) / docCount) / Math.log(docCount);
    }
}
