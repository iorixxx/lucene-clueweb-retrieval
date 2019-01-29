package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasks Track 2015
 * <a href="http://www.cs.ucl.ac.uk/tasks-track-2015">Tasks Track 2015</a>
 */
public class WT15 extends Track {

    private final int offset = 300;

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTask(Paths.get(home, "topics-and-qrels", "tasks_track_queries_2015.xml"), offset);
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMapForTask(Paths.get(home, "topics-and-qrels", "2015-qrels-docs.txt"), offset);
    }

    public WT15(String home) {
        this(home, Paths.get(home, "topics-and-qrels", "qrels.web.301-350.txt"));

    }

    WT15(String home, Path path) {
        super(home);
        try {
            saveQRelsMap(path);
        } catch (IOException ioe) {
            throw new RuntimeException("IO exception while saving qrels file for task track", ioe);
        }

    }

    private void saveQRelsMap(Path path) throws IOException {

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.US_ASCII));

        for (Map.Entry<Integer, Map<String, Integer>> entry : map.entrySet()) {

            int queryID = entry.getKey();

            Map<String, Integer> innerMap = entry.getValue();

            for (Map.Entry<String, Integer> e : innerMap.entrySet()) {
                String docID = e.getKey();
                int judge = e.getValue();

                if (judge < -2 || judge > 2)
                    throw new IllegalArgumentException("unexpected judge level for task track [-2, 0, 1, 2] " + judge);

                output.println(queryID + " 0 " + docID + " " + judge);
            }
        }

        output.flush();
        output.close();
    }

    /**
     * Document qrels for Task Track
     *
     * @param qrels Path of the qrels file
     * @throws IOException if any during file read operation
     */
    void populateQRelsMapForTask(Path qrels, int offset) throws IOException {

        final List<String> lines = Files.readAllLines(qrels, StandardCharsets.US_ASCII);

        for (String line : lines) {

            line = line.trim();

            String[] parts = whiteSpaceSplitter.split(line);

            if (parts.length != 5) throw new RuntimeException("qrels file should contain five columns : " + line);

            int queryID = Integer.parseInt(parts[0]) + offset;
            String docID = parts[2];
            int judge = Integer.parseInt(parts[3]);

            /*
            This year some documents were not rendered properly at time of evaluation at NIST.
            Such documents were assigned -3 label by the assessors.
            Since, document relevance was not known, we ignored these documents from evaluating for relevance.
             */
            if (-3 == judge) continue;


            judgeLevels.add(judge);

            Map<String, Integer> innerMap = map.getOrDefault(queryID, new HashMap<>());

            /*
            For the task completion, given a query, NIST assessors assigned document multiple relevance grades, each for possible tasks provided in ground truth.
            For Adhoc, we derived document relevance by using the maximum relevance label assigned for that document over all possible tasks.
            */
            if (innerMap.containsKey(docID)) {

                if (innerMap.get(docID) == -2) {
                    if (judge != -2) throw new RuntimeException("all subtopics must be spam! " + judge + " " + docID);
                }

                if (judge == -2) {
                    if (innerMap.get(docID) != -2)
                        throw new RuntimeException("+++ subtopics must be spam! " + innerMap.get(docID) + " " + docID);
                }

                if (judge > innerMap.get(docID))
                    innerMap.put(docID, judge);
            } else
                innerMap.put(docID, judge);

            map.put(queryID, innerMap);


        }

        lines.clear();
    }


    /**
     * Reads topics file in the format of Task Track (TT)
     *
     * @param topicsPath topics file
     * @param offset     task track topics start from 1, add with offset (e.g. 300) to make them distinct
     * @throws IOException read file
     */
    void populateInfoNeedsTask(Path topicsPath, int offset) throws IOException {

        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);

        String number = "";
        String query = "";
        for (String line : lines) {

            line = line.trim();


            if (line.startsWith("<tasktrack") || line.startsWith("</tasktrack")) continue;

            if (line.startsWith("<task")) {
                number = extract(line, "id");
            }

            if (line.startsWith("<query>") && line.endsWith("</query>"))
                query = line.substring(7, line.length() - 8).trim();


            if (line.startsWith("</task>")) {

                int qID;

                try {
                    qID = Integer.parseInt(number) + offset;
                } catch (java.lang.NumberFormatException nfe) {
                    throw new RuntimeException(line + " " + number + " " + query, nfe);
                }

                if (!isJudged(qID)) {
                    System.out.println(qID + ":" + query + " is not judged. Skipping...");
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

}


