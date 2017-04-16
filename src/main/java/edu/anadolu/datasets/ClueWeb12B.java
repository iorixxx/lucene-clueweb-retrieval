package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;
import org.clueweb09.tracks.WT13;
import org.clueweb09.tracks.WT14;

/**
 * ClueWeb12 B13 Dataset
 * Number of documents:	52,343,021
 */
public class ClueWeb12B extends DataSet {

    public ClueWeb12B(String tfd_home) {
        super(Collection.CW12B, new Track[]{
                new WT13(tfd_home),
                new WT14(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "clueweb12-000000-00-00000";
    }
}
