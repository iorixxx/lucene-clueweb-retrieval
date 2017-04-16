package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * TREC 2004 Terabyte Track
 * http://trec.nist.gov/data/terabyte/04/04.guidelines.html
 */
public class TT04 extends Track {

    public TT04(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTT(Paths.get(home, "topics-and-qrels", "topics.701-750.txt"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.701-750.txt"));
    }

    @Override
    public int getTopN() {
        return 10000;
    }
}
