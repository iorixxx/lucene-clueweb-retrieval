package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TREC 2007 Million Query Track
 * <p>
 * http://trec.nist.gov/data/million.query07.html
 */
public class MQ07 extends MQ08 {

    public MQ07(String home) {
        super(home);
    }

    @Override
    protected void checkQueryIDRange(int queryID) {
        if (queryID < 1 || queryID > 10000)
            throw new IllegalArgumentException("queryID: " + queryID + " must be between 1 and 10000");
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "prels.1-10000"));
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        Path queryPath = Paths.get(home, "topics-and-qrels", "07.million-query-topics.1-10000");
        populateInfoNeeds(queryPath);
    }
}
