package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extensively judged topics from 20251 to 20750 of the
 * <p>
 * TREC 2009 Million Query (1MQ) Track
 * <p>
 * http://ir.cis.udel.edu/million/data.html
 */
public class MQE2 extends DataSet {

    MQE2(String tfd_home) {
        super(Collection.MQE2, new Track[]{
                new org.clueweb09.tracks.MQE2(tfd_home)
        }, tfd_home);
    }

    @Override
    public Path indexesPath() {
        return Paths.get(tfd_home, Collection.CW09B.toString(), "indexes");
    }

    @Override
    public String getNoDocumentsID() {
        return "clueweb09-en0000-00-00000";
    }

    @Override
    public boolean spamAvailable() {
        return true;
    }

}
