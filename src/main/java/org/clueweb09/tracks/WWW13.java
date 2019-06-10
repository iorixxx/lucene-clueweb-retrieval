package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * The NTCIR-13  We Want Web-1 (WWW) Task!
 * http://www.thuir.cn/ntcirwww/
 */
public class WWW13 extends Track {

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWWW(Paths.get(home, "topics-and-qrels", "eng.queries.xml"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "wwwE.qrels"));
    }

    public WWW13(String home) {
        this(home, Paths.get(home, "topics-and-qrels", "qrels.www.1-100.txt"));

    }

    private WWW13(String home, Path path) {
        super(home);
        try {
            saveQRelsMap(path);
        } catch (IOException ioe) {
            throw new RuntimeException("IO exception while saving qrels file for www task", ioe);
        }

    }


    protected Triple processQRelLine(String line) {
        String[] parts = whiteSpaceSplitter.split(line);

        if (parts.length != 3) throw new RuntimeException("qrels file should contain three columns : " + line);

        int queryID = Integer.parseInt(parts[0]);

        String docID = parts[1];

        final int judge;
        switch (parts[2]) {
            case "L0":
                judge = 0;
                break;
            case "L1":
                judge = 1;
                break;
            case "L2":
                judge = 2;
                break;
            case "L3":
                judge = 3;
                break;
            case "L4":
                judge = 4;
                break;
            default:
                throw new AssertionError(this + line);

        }
        return new Triple(queryID, docID, judge);
    }

    /**
     * Reads topics file in the format of Web Track (WT)
     *
     * @param topicsPath topics file
     * @throws IOException exception
     */
    private void populateInfoNeedsWWW(Path topicsPath) throws IOException {

        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);


        String number = "";

        String query = "";
        for (String line : lines) {

            line = line.trim();

            if ("<query>".equals(line)) {
                number = null;
                query = null;
                continue;
            }

            if (line.startsWith("<qid>") && line.endsWith("</qid>"))
                number = line.substring(5, line.length() - 6).trim();

            if (line.startsWith("<content>") && line.endsWith("</content>"))
                query = line.substring(9, line.length() - 10).trim();

            if ("</query>".equals(line)) {

                if (number == null || query == null) throw new RuntimeException("quid or query is null!");

                int qID = Integer.parseInt(number);
                if (!isJudged(qID)) {
                    System.out.println(number + ":" + query + " is not judged. Skipping...");
                    continue;
                }

                final Map<String, Integer> innerMap = map.get(qID);

                InfoNeed need = new InfoNeed(qID, query, this, innerMap);


                if (need.relevant() == 0) {
                    System.out.println(need.toString() + " does not have relevant documents. Skipping...");
                    continue;
                }
                needs.add(need);
            }
        }

        lines.clear();
    }

    private void saveQRelsMap(Path path) throws IOException {

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.US_ASCII));

        for (Map.Entry<Integer, Map<String, Integer>> entry : map.entrySet()) {

            int queryID = entry.getKey();

            Map<String, Integer> innerMap = entry.getValue();

            for (Map.Entry<String, Integer> e : innerMap.entrySet()) {
                String docID = e.getKey();
                int judge = e.getValue();

                if (judge < 0 || judge > 4)
                    throw new IllegalArgumentException("unexpected judge level for WWW task [0, 1, 2, 3, 4] " + judge);

                output.println(queryID + " 0 " + docID + " " + judge);
            }
        }

        output.flush();
        output.close();
    }
}
