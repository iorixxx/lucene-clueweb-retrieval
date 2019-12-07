package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

public class UBE extends MC {

    public UBE(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws Exception {
        populateInfoNeeds("queriesUBE.csv");
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrelsUBE.txt"));
    }
}