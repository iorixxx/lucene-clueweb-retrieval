package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Gokhan on 11.07.2017.
 */
public class MC extends Track {
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not found!");
        }
    }

    public MC (String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:h2:file:" + home, "sa", "");
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT QueryID, Topic from queries");
        while (rs.next()) {
            int qID = rs.getInt(1);
            String query = rs.getString(2);

            final Map<String, Integer> innerMap = map.get(qID);

            InfoNeed need = new InfoNeed(qID, MQ09.escape(query), this, innerMap);


            if (need.relevant() == 0) {
                //System.out.println(qID + ":" + query + " does not have relevant documents. Skipping...");
                continue;
            }
            needs.add(need);
        }
        conn.close();
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:h2:file:" + home, "sa", "");
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT QueryID, docno, rel from qrels");
        while (rs.next()) {
            int queryID = rs.getInt(1);
            String docID =  rs.getString(2);
            int judge = rs.getInt(3);
            final Triple triple = new Triple(queryID, docID, judge);

            judgeLevels.add(triple.judge);

            if (map.containsKey(triple.queryID)) {
                Map<String, Integer> innerMap = map.get(triple.queryID);
                innerMap.put(triple.docID, triple.judge);
            } else {
                map.put(triple.queryID, new HashMap<>());
            }
        }
        conn.close();
    }

    @Override
    protected int getTopN() {
        return 1000;
    }
}
