package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TREC 2007 Million Query Track
 * <p>
 * http://trec.nist.gov/data/million.query07.html
 */
public class MQ07 extends DataSet {

    MQ07(String tfd_home) {
        super(Collection.MQ07, new Track[]{
                new org.clueweb09.tracks.MQ07(tfd_home)
        }, tfd_home);
    }

    @Override
    public Path indexesPath() {
        return Paths.get(tfd_home, Collection.GOV2.toString(), "indexes");
    }

    @Override
    public String getNoDocumentsID() {
        return "GX000-00-0000000";
    }
}
