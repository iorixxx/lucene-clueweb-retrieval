package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

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
     * Kullback-Leibler between a (simplified)
     * query language model and a collection model
     */
    public double value(InfoNeed need) throws IOException {

        double scs = 0.0;
        double qtf, ctf;

        String terms[] = whiteSpaceSplitter.split(need.query());
        List<String> termList = new ArrayList<>(Arrays.asList(terms));

        for (String term: terms) {
            qtf = (double) Collections.frequency(termList, term) / terms.length;
            ctf = (double) ctf(field, term) / sumTotalTermFreq;
            scs += qtf * Math.log(qtf / ctf);
        }

        return scs;
    }

    public String toString() {
        return "SCS";
    }
}
