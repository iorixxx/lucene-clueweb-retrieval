package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;


/**
 * Tasks Track 2016
 * <a href="http://trec.nist.gov/pubs/trec25/papers/Overview-T.pdf">Overview of the TREC Tasks Track 2016</a>
 */

public final class WT16 extends WT15 {

    private final int offset = 350;

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTask(Paths.get(home, "topics-and-qrels", "tasks_track_queries_2016.xml"), offset);
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMapForTask(Paths.get(home, "topics-and-qrels", "2016-qrels-docs.txt"), offset);
    }

    public WT16(String home) {
        super(home, Paths.get(home, "topics-and-qrels", "qrels.web.351-400.txt"));
    }
}