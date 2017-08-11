package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 2013 Web Track topics (query field only)
 * http://trec.nist.gov/data/web/2013/web2013.topics.txt
 */
public class WT13 extends Track {

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWT(Paths.get(home, "topics-and-qrels", "topics.web.201-250.txt"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.web.201-250.txt"));
    }

    public WT13(String home) {
        super(home);
    }

}
