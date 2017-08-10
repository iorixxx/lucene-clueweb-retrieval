package edu.anadolu.datasets;

import org.clueweb09.tracks.TT04;
import org.clueweb09.tracks.TT05;
import org.clueweb09.tracks.TT06;
import org.clueweb09.tracks.Track;

/**
 * GOV2 Test Collection
 * http://ir.dcs.gla.ac.uk/test_collections/gov2-summary.htm
 */
class Gov2 extends DataSet {

    Gov2(String tfd_home) {
        super(Collection.GOV2, new Track[]{
                new TT04(tfd_home),
                new TT05(tfd_home),
                new TT06(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "GX000-00-0000000";
    }

    @Override
    public boolean spamAvailable() {
        return false;
    }
}
