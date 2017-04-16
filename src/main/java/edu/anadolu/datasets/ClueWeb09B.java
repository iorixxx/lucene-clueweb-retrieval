package edu.anadolu.datasets;

import org.clueweb09.tracks.*;

/**
 * The ClueWeb09 Category B Dataset
 */
public class ClueWeb09B extends DataSet {
    public ClueWeb09B(String tfd_home) {
        super(Collection.CW09B, new Track[]{
                new WT09(tfd_home),
                new WT10(tfd_home),
                new WT11(tfd_home),
                new WT12(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "clueweb09-en0000-00-00000";
    }
}
