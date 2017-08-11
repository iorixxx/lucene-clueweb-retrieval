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
 * TREC 2009 Million Query (1MQ) Track
 * <p>
 * http://ir.cis.udel.edu/million/data.html
 */
public class MQ09 extends Track {

    /**
     * Returns a String where those characters that QueryParser
     * expects to be escaped are replaced by a single whitespace.
     */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append(' ');
            } else
                sb.append(c);
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    /**
     * P is the priority (a number 1-4, with 1 indicating highest priority)
     */
    static final int P = 1;

    /**
     * @param home tfd.home directory
     */
    public MQ09(String home) {
        super(home);
    }


    protected void checkQueryIDRange(int queryID) {
        if (queryID < 20001 || queryID > 60000)
            throw new IllegalArgumentException("queryID: " + queryID + " must be between 20001 and 60000");
    }

    @Override
    protected Triple processQRelLine(String line) {
        String[] parts = whiteSpaceSplitter.split(line);

        assert parts.length == 5 : "prels file should contain five columns : " + line;

        int queryID = Integer.parseInt(parts[0]);
        checkQueryIDRange(queryID);
        String docID = parts[1];
        int judge = Integer.parseInt(parts[2]);

        return new Triple(queryID, docID, judge);
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "prels.20001-60000"));
    }

    @Override
    protected void populateInfoNeeds() throws IOException {

        Path queryPath = Paths.get(home, "topics-and-qrels", "09.mq.topics.20001-60000");
        List<String> lines = Files.readAllLines(queryPath, StandardCharsets.ISO_8859_1);

        for (String line : lines) {
            String parts[] = line.split(":");
            assert parts.length == 3 : "topic does not contain colon : " + line;

            int qID = Integer.parseInt(parts[0]);
            checkQueryIDRange(qID);
            // P is the priority (a number 1-4, with 1 indicating highest priority)
            int priority = Integer.parseInt(parts[1]);
            if (priority > P) break;

            String query = parts[2].trim();


            if (!isJudged(qID)) {
                // System.out.println(qID + ":" + query + " is not judged. Skipping...");
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
