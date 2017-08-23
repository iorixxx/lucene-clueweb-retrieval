package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * TREC 2005 Terabyte Track
 * http://plg.uwaterloo.ca/~claclark/TB05.html
 */
public class TT05 extends Track {

    public TT05(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTT(Paths.get(home, "topics-and-qrels", "topics.751-800.txt"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.751-800.txt"));
    }
}
