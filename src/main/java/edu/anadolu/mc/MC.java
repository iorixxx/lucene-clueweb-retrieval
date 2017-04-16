package edu.anadolu.mc;

import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Map;

/**
 * 72 queries for Milliyet Collection
 */
public class MC extends Track {

    public MC(String home) {
        super(home);
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrelsMC.txt"));
    }

    @Override
    protected void populateInfoNeeds() throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/milliyetcollection", "root", "");
        Statement statement = conn.createStatement();

        ResultSet rs = statement.executeQuery("SELECT * from queries");

        while (rs.next()) {
            int qID = rs.getInt(1);
            String query = rs.getString(2);

            if (!isJudged(qID)) {
                System.out.println(qID + ":" + query + " is not judged. Skipping...");
                continue;
            }

            final Map<String, Integer> innerMap = map.get(qID);

            InfoNeed need = new InfoNeed(qID, query, this, innerMap);

            if (need.relevant() == 0) {
                System.out.println(qID + ":" + query + " does not have relevant documents. Skipping...");
                continue;
            }
            needs.add(need);
        }

        rs.close();
        statement.close();
        conn.close();
    }

    @Override
    protected int getTopN() {
        return 1000;
    }
}
