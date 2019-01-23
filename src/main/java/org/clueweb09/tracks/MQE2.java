package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.clueweb09.tracks.MQ09.escape;

/**
 * Extensively judged topics from 20251 to 20708 of the
 * <p>
 * TREC 2009 Million Query (1MQ) Track
 * <p>
 * http://ir.cis.udel.edu/million/data.html
 */
public class MQE2 extends Track {

    /**
     * @param home tfd.home directory
     */
    public MQE2(String home) {
        super(home);
    }


    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "09.mq.qrels.20251-20750.deep.judged.txt"));
    }

    @Override
    protected void populateInfoNeeds() throws IOException {

        Path queryPath = Paths.get(home, "topics-and-qrels", "09.mq.topics.20001-60000");
        List<String> lines = Files.readAllLines(queryPath, StandardCharsets.ISO_8859_1);

        for (String line : lines) {
            String parts[] = line.split(":");
            assert parts.length == 3 : "topic does not contain colon : " + line;

            int qID = Integer.parseInt(parts[0]);

            // P is the priority (a number 1-4, with 1 indicating highest priority)
            if (qID <= 20250) continue;
            if (qID > 20708) break;

            String query = parts[2].trim();

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
