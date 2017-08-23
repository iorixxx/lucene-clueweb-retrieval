package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 2006 TREC Terabyte Track
 * http://plg.uwaterloo.ca/~claclark/TB06.html
 */
public class TT06 extends Track {

    public TT06(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTT(Paths.get(home, "topics-and-qrels", "topics.801-850.txt"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.801-850.txt"));
    }
}
