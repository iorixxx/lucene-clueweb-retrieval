package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;
import org.clueweb09.tracks.UBE;

public class MC extends DataSet {

    public MC(String tfd_home) {
        super(Collection.MC, new Track[]{
                new org.clueweb09.tracks.MC(tfd_home)
            //    new UBE(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "Milliyet_0105_v00_00000";
    }

    @Override
    public boolean spamAvailable() {
        return false;
    }
}
