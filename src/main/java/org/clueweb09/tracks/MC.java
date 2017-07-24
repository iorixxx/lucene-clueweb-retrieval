package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MC extends Track {


    public MC (String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws Exception {
        Path topicsPath=Paths.get(home, "topics-and-qrels", "mctopics.txt");
        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);

        Iterator<String> iterator = lines.iterator();

        while (iterator.hasNext()) {

            final String line = iterator.next().trim();
            String[] parts = line.split(":");
            int qID = Integer.parseInt(parts[0]);
            String query = parts[1];
            if (!isJudged(qID)) {
                System.out.println(qID + ":" + query + " is not judged. Skipping...");
                continue;
            }

            final Map<String, Integer> innerMap = map.get(qID);

            InfoNeed need = new InfoNeed(qID, MQ09.escape(query), this, innerMap);

            if (need.relevant() == 0) {
                System.out.println(need.toString() + " does not have relevant documents. Skipping...");
                continue;
            }
            needs.add(need);
        }


        lines.clear();

    }

    @Override
    protected void populateQRelsMap() throws Exception {
        Path qrelsPath=Paths.get(home, "topics-and-qrels", "mcqrels.txt");
        final List<String> qrels = Files.readAllLines(qrelsPath, StandardCharsets.UTF_8);

        for (String l : qrels) {
            String[] parts = whiteSpaceSplitter.split(l);
            assert parts.length == 4 : "qrels file should contain four columns : " + l;
            int queryID = Integer.parseInt(parts[0]);

            String docID = parts[2];
            int judge = Integer.parseInt(parts[3]);

            final Triple t= new Triple(queryID, docID, judge);
            judgeLevels.add(t.judge);
            if (map.containsKey(t.queryID)) {
                Map<String, Integer> innerMap = map.get(t.queryID);
                innerMap.put(t.docID, t.judge);
            } else {
                map.put(t.queryID, new HashMap<>());
            }
        }

        qrels.clear();
    }

    @Override
    protected int getTopN() {
        return 1000;
    }
}
