package edu.anadolu.qpp;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Query Scope
 */
public class Scope extends Base {

    private final QueryParser queryParser;

    public Scope(Path indexPath) throws IOException {
        super(indexPath, "contents");
        queryParser = new QueryParser(field, Analyzers.analyzer(Tag.tag(indexPath.getFileName().toString())));
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
    }


    /**
     * the number of documents containing at least one of the query terms
     */
    private int scope(InfoNeed need) throws IOException, ParseException {
        return searcher.count(queryParser.parse(need.query()));
    }

    @Override
    public double value(String word) throws IOException {
        throw new UnsupportedOperationException();
    }

    public double value(InfoNeed need) throws IOException, ParseException {
        return -Math.log((double) scope(need) / docCount) / Math.log(docCount);
    }
}
