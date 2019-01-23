package edu.anadolu.qpp;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Collection Query Similarity
 */
public class SCQ extends Base {

    public SCQ(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    /**
     * Collection Query Similarity (SCQ) of a term
     */
    @Override
    public double value(String word) throws IOException {

        Double idf = Math.log((double) docCount / df(field, word));

        return (1 + Math.log((ctf(field, word)))) * idf;
    }

    public String toString() {
        return "SCQ";
    }
}
