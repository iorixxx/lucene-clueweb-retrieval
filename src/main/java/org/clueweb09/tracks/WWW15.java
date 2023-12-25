package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * The NTCIR-15 WWW-3 English Subtask
 * http://sakailab.com/www3english/
 */
public class WWW15 extends WWW13 {

    @Override
    protected int offset() {
        return 100;
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWWW(Paths.get(home, "topics-and-qrels", "www3topics-E.xml"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        // tail -n -16677 ntcir15www2+3official.qrels >> ntcir15www3.qrels
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "ntcir15www3.qrels"));
    }

    public WWW15(String home) {
        super(home, Paths.get(home, "topics-and-qrels", "qrels.www.201-280.txt"));
    }
}
