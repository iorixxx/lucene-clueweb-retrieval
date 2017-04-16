package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * TREC 2004 Robust Track
 * http://trec.nist.gov/data/t13_robust.html
 */
public class ROB04 extends Track {

    public ROB04(String home) {
        super(home);
    }

    @Override
    public void populateInfoNeeds() throws IOException {
        populateInfoNeedsTT(Paths.get(home, "topics-and-qrels", "04.testset"));
    }

    @Override
    public void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.robust2004.txt"));
    }

    @Override
    public int getTopN() {
        return 1000;
    }
}
