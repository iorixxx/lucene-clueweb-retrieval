package org.clueweb09.tracks;

/**
 * Class to hold queryID, docID, judge triples
 */
final public class Triple {

    final int queryID;
    final String docID;
    final int judge;

    public Triple(int queryID, String docID, int judge) {
        this.queryID = queryID;
        this.docID = docID;
        this.judge = judge;
    }
}
