package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;
import org.clueweb09.tracks.WWW13;
import org.clueweb09.tracks.WWW14;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The NTCIR-13  We Want Web-1 (WWW) Task!
 * http://www.thuir.cn/ntcirwww/
 * <p>
 * The NTCIR-14 We Want Web-2 (WWW) Task!
 * http://www.thuir.cn/ntcirwww2/
 * <p>
 * The NTCIR-15 We Want Web-3 (WWW) Task!
 */

public class NTCIR extends DataSet {

    NTCIR(String tfd_home) {
        super(Collection.NTCIR, new Track[]{
                new WWW13(tfd_home), new WWW14(tfd_home)
        }, tfd_home);
    }

    @Override
    public Path indexesPath() {
        return Paths.get(tfd_home, Collection.CW12B.toString(), "indexes");
    }

    @Override
    public String getNoDocumentsID() {
        return "clueweb12-0000wb-00-00000";
    }

    @Override
    public boolean validateDocID(String docID) {
        return docID.startsWith("clueweb12-");
    }

    @Override
    public boolean spamAvailable() {
        return true;
    }
}
