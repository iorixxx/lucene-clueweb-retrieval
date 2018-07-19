package edu.anadolu.datasets;

import org.clueweb09.tracks.*;

public class ClueWeb12A extends DataSet {

    ClueWeb12A(String tfd_home) {
        super(Collection.CW12A, new Track[]{
                new WT13(tfd_home),
                new WT14(tfd_home),
                new WT15(tfd_home),
                new WT16(tfd_home)
        }, tfd_home);
    }


    /**
     * TREC submission system requires you to submit documents for every topic.
     * If there are no documents for a certain topic, please insert 'clueweb12-0000wb-00-00000' as DOC-ID with a dummy score.
     */
    @Override
    public String getNoDocumentsID() {
        return "clueweb12-0000wb-00-00000";
    }

    @Override
    public boolean spamAvailable() {
        return true;
    }
}
