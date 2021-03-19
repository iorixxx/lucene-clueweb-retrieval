package edu.anadolu.datasets;

import org.clueweb09.tracks.*;

/**
 * The ClueWeb09 Category B Dataset
 */
class ClueWeb09B extends DataSet {
    ClueWeb09B(String tfd_home) {
        super(Collection.CW09B, new Track[]{
                new WT09(tfd_home),
                new WT10(tfd_home),
                new WT11(tfd_home),
                new WT12(tfd_home)
        }, tfd_home);
    }

    /**
     * If you would normally return no documents for a query, instead return the single document "clueweb09-en0000-00-00000" at rank one.
     * Doing so maintains consistent evaluation results (averages over the same number of queries) and does not break anyone's tools.
     */
    @Override
    public String getNoDocumentsID() {
        return "clueweb09-en0000-00-00000";
    }

    @Override
    public boolean validateDocID(String docID) {
        return docID.startsWith("clueweb09-");
    }

    @Override
    public boolean spamAvailable() {
        return true;
    }
}
