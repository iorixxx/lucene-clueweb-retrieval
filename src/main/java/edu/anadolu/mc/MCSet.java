package edu.anadolu.mc;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.DataSet;
import org.clueweb09.tracks.Track;

/**
 * Milliyet Collection and its query set
 */
public class MCSet extends DataSet {

    public MCSet(String tfd_home) {
        super(Collection.MC, new Track[]{new MC(tfd_home)}, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "Milliyet_0105_v00_00000";
    }

}
