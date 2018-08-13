package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Encapsulates TREC Tracks that use ClueWeb09B dataset.
 */
public abstract class Track implements Comparable<Track> {

    protected final Map<Integer, Map<String, Integer>> map = new TreeMap<>();

    final SortedSet<Integer> judgeLevels = new TreeSet<>();

    protected final List<InfoNeed> needs = new ArrayList<>();

    protected final String home;

    public Track(String home) {
        this.home = home;
        try {
            populateQRelsMap();
            populateInfoNeeds();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final Pattern whiteSpaceSplitter = Pattern.compile("\\s+");

    public Map<Integer, Map<String, Integer>> getMap() {
        return map;
    }

    @Override
    public int compareTo(Track o) {
        return this.toString().compareTo(o.toString());
    }

    protected abstract void populateInfoNeeds() throws Exception;

    protected abstract void populateQRelsMap() throws Exception;

    public List<InfoNeed> getTopics() {
        return Collections.unmodifiableList(needs);
    }

    public InfoNeed need(int queryID) {
        for (InfoNeed need : needs)
            if (need.id() == queryID) return need;
        return null;
    }

    public final SortedSet<Integer> getJudgeLevels() {
        return judgeLevels;
    }

    static String extract(String line, String tag) {

        int i = line.indexOf(tag);

        if (i == -1) throw new IllegalArgumentException("line does not contain the tag : " + tag);

        int j = line.indexOf("\"", i + tag.length());

        if (j == -1) throw new IllegalArgumentException("line does not contain quotation: " + line);

        int k = line.indexOf("\"", j + 1);

        if (k == -1) throw new IllegalArgumentException("line does not contain ending quotation: " + line);

        return line.substring(j + 1, k).trim();

    }

    protected void populateQRelsMap(Path qrels) throws IOException {

        final List<String> lines = Files.readAllLines(qrels, StandardCharsets.US_ASCII);

        for (String line : lines) {

            final Triple triple = processQRelLine(line);

            judgeLevels.add(triple.judge);

            Map<String, Integer> innerMap = map.getOrDefault(triple.queryID, new HashMap<>());

            innerMap.put(triple.docID, triple.judge);

            map.put(triple.queryID, innerMap);
        }

        lines.clear();
    }

    protected Triple processQRelLine(String line) {
        String[] parts = whiteSpaceSplitter.split(line);

        assert parts.length == 4 : "qrels file should contain four columns : " + line;

        int queryID = Integer.parseInt(parts[0]);

        String docID = parts[2];
        int judge = Integer.parseInt(parts[3]);

        return new Triple(queryID, docID, judge);
    }

    /**
     * Reads topics file in the format of Web Track (WT)
     *
     * @param topicsPath topics file
     * @throws IOException
     */
    protected void populateInfoNeedsWT(Path topicsPath) throws IOException {

        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);

        int subTopicCount = 0;
        String number = "";
        String type = "";
        String query = "";
        for (String line : lines) {

            line = line.trim();

            if (line.startsWith("<topic")) {
                number = extract(line, "number");
                type = extract(line, "type");
                subTopicCount = 0;
            }

            if (line.startsWith("<query>") && line.endsWith("</query>"))
                query = line.substring(7, line.length() - 8).trim();

            if (line.startsWith("<subtopic"))
                subTopicCount++;

            if (line.startsWith("</topic>")) {
                // System.out.println(number + "\t" + type + "\t" + subTopicCount);

                int qID = Integer.parseInt(number);
                if (!isJudged(qID)) {
                    System.out.println(number + ":" + query + " is not judged. Skipping...");
                    continue;
                }

                final Map<String, Integer> innerMap = map.get(qID);

                InfoNeed need = new InfoNeed(qID, query, this, innerMap);
                need.setType(type);
                need.setSubTopicCount(subTopicCount);

                if (need.relevant() == 0) {
                    System.out.println(need.toString() + " does not have relevant documents. Skipping...");
                    continue;
                }
                needs.add(need);
            }
        }

        lines.clear();
    }


    /**
     * Reads topics file in the format of Terabyte Track (TT)
     *
     * @param topicsPath topics file
     * @throws IOException
     */
    protected void populateInfoNeedsTT(Path topicsPath) throws IOException {

        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);

        String number = "";
        String query = "";

        boolean found = false;

        Iterator<String> iterator = lines.iterator();

        while (iterator.hasNext()) {

            final String line = iterator.next().trim();

            if (!found && "<top>".equals(line)) {
                found = true;
                continue;
            }

            if (found && line.startsWith("<title>")) {
                query = line.substring(7).trim();
                if (query.length() == 0)
                    query = iterator.next().trim();
            }

            if (found && line.startsWith("<num>")) {
                int i = line.lastIndexOf(" ");
                if (-1 == i) throw new RuntimeException("cannot find space in : " + line);
                number = line.substring(i).trim();
            }

            if (found && "</top>".equals(line)) {
                found = false;
                int qID = Integer.parseInt(number);
                if (!isJudged(qID)) {
                    System.out.println(number + ":" + query + " is not judged. Skipping...");
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
        }

        lines.clear();

    }

    public boolean isJudged(int queryID) {
        return map.containsKey(queryID);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
