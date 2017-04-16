package edu.anadolu.qpp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inverse Collection Term Frequency (ICTF)
 */
public class ICTF extends Base {

    public ICTF(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    /**
     * Inverse Collection Term Frequency (ictf) of a term
     */
    @Override
    public double value(String word) throws IOException {
        return Math.log(sumTotalTermFreq / ctf(field, word));
    }

    public static void main(String[] args) throws IOException {
        try (Base ictf = new ICTF(Paths.get("/Volumes/clueweb09/indexes/KStemAnalyzer"))) {
            display(ictf, new Aggregate.Variance());
        }
    }
}
