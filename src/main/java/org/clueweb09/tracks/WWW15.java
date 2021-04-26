package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

public class WWW15 extends WWW13{

    @Override
    protected int offset() {
        return 200;
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWWW(Paths.get(home, "topics-and-qrels", "www3topics-E.xml"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "www3e.qrels"));
    }

    public WWW15(String home) {
        super(home, Paths.get(home, "topics-and-qrels", "qrels.www.201-280.txt"));
    }
}