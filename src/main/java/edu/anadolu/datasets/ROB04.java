package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;

/**
 * TREC 2004 Robust Track (ROB04)
 * <p>
 * http://trec.nist.gov/data/robust.html
 */
class ROB04 extends DataSet {

    ROB04(String tfd_home) {
        super(Collection.ROB04, new Track[]{
                new org.clueweb09.tracks.ROB04(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "LA000000-0040";
    }

    @Override
    public boolean spamAvailable() {
        return false;
    }
}
