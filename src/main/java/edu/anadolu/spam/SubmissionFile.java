package edu.anadolu.spam;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Class to encapsulate TREC submission file
 */
public class SubmissionFile {

    private final Map<Integer, List<Tuple>> entries = new LinkedHashMap<>(50);

    private String tag;

    public String runTag() {
        return tag;
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public Map<Integer, List<Tuple>> entryMap() {
        return Collections.unmodifiableMap(entries);
    }

    public static class Tuple implements Comparable<Tuple> {

        final public String docID;
        final public String score;
        final private double s;

        public Tuple(String docID, String score) {
            this.docID = docID;
            this.score = score;
            this.s = Double.parseDouble(score);
        }

        public String docID() {
            return docID;
        }

        @Override
        public String toString() {
            return docID + " " + score;
        }

        @Override
        public int compareTo(Tuple o) {
            if (o.s < this.s) return -1;
            else if (o.s > this.s) return 1;
            else {
                return o.docID.compareTo(this.docID);
            }
        }
    }

    public SubmissionFile(Path submission) throws IOException {
        if (!Files.isRegularFile(submission) || !Files.exists(submission) || !Files.isReadable(submission))
            throw new IllegalArgumentException(submission + " does not exist or is not a regular file");


        try (BufferedReader reader = Files.newBufferedReader(submission, StandardCharsets.US_ASCII)) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;

                String[] parts = whiteSpaceSplitter.split(line);

                int qID = Integer.parseInt(parts[0]);
                String docID = parts[2];
                String score = parts[4];
                if (entries.containsKey(qID)) {
                    entries.get(qID).add(new Tuple(docID, score));
                } else {
                    List<Tuple> list = new ArrayList<>(10000);
                    list.add(new Tuple(docID, score));
                    entries.put(qID, list);
                }

                this.tag = parts[5];

            }
        }
    }

}
