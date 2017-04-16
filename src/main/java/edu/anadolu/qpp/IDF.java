package edu.anadolu.qpp;

import org.apache.lucene.index.DirectoryReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inverse Document Frequency (IDF)
 */
public class IDF extends Base {

    public IDF(DirectoryReader reader) throws IOException {
        super(reader, "contents");
    }

    public IDF(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    /**
     * Inverse Document Frequency (idf) of a term
     */
    @Override
    public double value(String word) throws IOException {
        return Math.log((double) docCount / df(field, word));
    }

    public static void main(String[] args) throws IOException {
        try (Base idf = new IDF(Paths.get("/Volumes/clueweb09/indexes/KStemAnalyzer"))) {
            display(idf, new Aggregate.Gamma1());
        }
    }
}
