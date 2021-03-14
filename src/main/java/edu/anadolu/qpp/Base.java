package edu.anadolu.qpp;

import edu.anadolu.QueryBank;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Base class for pre-retrieval predictors based on term specificity
 */
public abstract class Base implements Predictor, Closeable {

    protected final long sumTotalTermFreq;
    protected final long docCount;

    protected final String field;

    protected final IndexSearcher searcher;
    protected final DirectoryReader reader;

    protected final Path indexPath;
    protected final Analyzer analyzer;

    public Base(Path indexPath, String field) throws IOException {

        if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
            throw new IllegalArgumentException(indexPath + " does not exist or is not a directory.");
        }

        this.indexPath = indexPath;
        this.analyzer = Analyzers.analyzer(Tag.tag(indexPath.getFileName().toString()));
        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(new MetaTerm());
        this.field = field;

        System.out.println("Opened index directory : " + reader.directory().toString() + " has " + reader.numDocs() + " numDocs and has " + reader.maxDoc() + " maxDocs");

        CollectionStatistics collectionStatistics = searcher.collectionStatistics(field);
        docCount = collectionStatistics.docCount();
        sumTotalTermFreq = collectionStatistics.sumTotalTermFreq();

        System.out.println("docCount:" + docCount + " numDocs:" + reader.numDocs() + " sumTotalTermFreq:" + sumTotalTermFreq);

    }

    @Override
    public void close() throws IOException {
        reader.close();
    }


    /**
     * Document Frequency (df) of a term
     */
    public long df(String field, String word) throws IOException {
        Term term = new Term(field, word);
        TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
        if (termStatistics.docFreq() == 0) System.err.println(word + " has 0 df");
        return termStatistics.docFreq();
    }

    /**
     * Collection Term Frequency (ctf) of a term
     */
    public long ctf(String field, String word) throws IOException {
        Term term = new Term(field, word);
        TermStatistics termStatistics = searcher.termStatistics(term, TermContext.build(reader.getContext(), term));
        return termStatistics.totalTermFreq();
    }

    public static void display(Predictor predictor, Aggregate aggregate) throws IOException {
        QueryBank selector = new QueryBank(null);

        for (InfoNeed need : selector.getAllQueries(0)) {
            System.out.print("qid:");
            System.out.print(need.id());
            System.out.print("\t");
            System.out.print(predictor.aggregated(need, aggregate));
            System.out.println();
        }
    }

    @Override
    public final double aggregated(InfoNeed need, Aggregate aggregate) throws IOException {

        final List<String> terms = Analyzers.getAnalyzedTokens(need.query(), analyzer);
        final double[] values = new double[terms.size()];

        int c = 0;
        for (String word : terms) {
            values[c++] = value(word);
        }

        return aggregate.aggregate(values);
    }
}
