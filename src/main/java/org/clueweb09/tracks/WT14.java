package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 2014 Web Track topics (query field only)
 * http://trec.nist.gov/data/web/2014/web2014.topics.txt
 */
public class WT14 extends Track {

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWT(Paths.get(home, "topics-and-qrels", "topics.web.251-300.txt"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.web.251-300.txt"));
    }

    public WT14(String home) {
        super(home);
    }

    /**
     * An experimental run consists of the top 10,000 documents for 2014 Web Track
     * http://www-personal.umich.edu/~kevynct/trec-web-2014/
     *
     * @return 10, 000
     */
    @Override
    protected int getTopN() {
        return 10000;
    }
}
