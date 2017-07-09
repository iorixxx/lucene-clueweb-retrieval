package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * TREC 2008 Million Query Track
 * <p>
 * http://trec.nist.gov/data/million.query08.html
 */
public class MQ08 extends MQ09 {


    public MQ08(String home) {
        super(home);
    }


    @Override
    protected void checkQueryIDRange(int queryID) {
        if (queryID < 10001 || queryID > 20000)
            throw new IllegalArgumentException("queryID: " + queryID + " must be between 10001 and 20000");
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "prels.10001-20000"));
    }

    @Override
    protected void populateInfoNeeds() throws IOException {

        Path queryPath = Paths.get(home, "topics-and-qrels", "08.million-query-topics.10001-20000");
        List<String> lines = Files.readAllLines(queryPath, StandardCharsets.ISO_8859_1);

        for (String line : lines) {

            int i = line.indexOf(":");

            if (i == -1) throw new RuntimeException(line + " line does not contain colon :");


            int qID = Integer.parseInt(line.substring(0, i));
            checkQueryIDRange(qID);


            String query = line.substring(i + 1);


            if (!isJudged(qID)) {
                //System.out.println(qID + ":" + query + " is not judged. Skipping...");
                continue;
            }

            final Map<String, Integer> innerMap = map.get(qID);

            InfoNeed need = new InfoNeed(qID, escape(query), this, innerMap);


            if (need.relevant() == 0) {
                //System.out.println(qID + ":" + query + " does not have relevant documents. Skipping...");
                continue;
            }
            needs.add(need);

        }
        lines.clear();
    }
}
