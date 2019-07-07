package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * The NTCIR-14  We Want Web-2 (WWW) Task!
 * http://www.thuir.cn/ntcirwww2/
 */
public class WWW14 extends WWW13 {

    @Override
    protected int offset() {
        return 100;
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWWW(Paths.get(home, "topics-and-qrels", "qEng2.xml"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "www2e.qrels"));
    }

    public WWW14(String home) {
        super(home, Paths.get(home, "topics-and-qrels", "qrels.www.101-180.txt"));
    }
}
