package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Firstly, attack on one term queries (otq)
 */
public final class Exporter {

    private final DataSet dataSet;
    private final String tag;
    private final long numberOfDocuments;

    public final int DOC_ID_LENGTH;

    public Exporter(DataSet dataSet, String tag) {
        this.dataSet = dataSet;
        this.tag = tag;

        String line = Evaluator.loadCorpusStats(dataSet.collectionPath(), "contents", tag);
        String[] parts = whiteSpaceSplitter.split(line);

        if (parts.length != 4)
            throw new RuntimeException("line from field_stats.csv does not have four parts " + line);

        numberOfDocuments = Long.parseLong(parts[2]);

        DOC_ID_LENGTH = dataSet.getNoDocumentsID().length();
    }


    /**
     * @param need
     * @param field
     * @param judge
     * @param word
     * @return a Map whose keys are docIDs values are relative frequencies
     * @throws IOException
     */
    public Map<String, Double> getRelativeFreqList(InfoNeed need, String field, int judge, String word) throws IOException {

        Path freqPath = dataSet.collectionPath().resolve("freqs");
        Track track = need.getWT();

        Path path = freqPath.resolve(tag).resolve(track.toString()).resolve(field + "_" + Integer.toString(judge) + "_freq.csv");
        if (!Files.exists(path)) throw new RuntimeException(path + "  does not exists!");

        List<String> lines = Files.readAllLines(path);

        Map<String, Double> map = new HashMap<>();

        boolean add = false;
        for (String line : lines) {

            if (line.trim().equals(need.id() + ":" + word)) {

                add = true;
                continue;
            }

            if (add && line.contains(":"))
                break;

            if (add) {
                String[] parts = line.split("=");
                // keys are docIDs, values are relative frequencies
                map.put(parts[0], calculateRelativeFreq(parts[1]));

            }


        }

        if (!add)
            throw new RuntimeException(need.id() + ":" + word + " not found in " + path.toString());

        lines.clear();
        return map;
    }

    /**
     * makes calculation from string e.g. 12/527
     *
     * @param s for example 12/527
     * @return double relative frequency
     */
    double calculateRelativeFreq(String s) {
        String[] parts = s.split("/");
        if (parts.length != 2) throw new RuntimeException("parts not two length!");
        return (double) Integer.parseInt(parts[0]) / Long.parseLong(parts[1]);
    }


    static class Triple implements Comparable<Triple> {
        final int judge;
        final String docID;
        final double score;

        public Triple(int judge, String docID, double score) {
            this.docID = docID;
            this.judge = judge;
            this.score = score;
        }

        @Override
        public String toString() {
            return docID + " " + judge + " " + score;
        }

        @Override
        public int compareTo(Triple o) {
            if (o.judge < this.judge) return -1;
            else if (o.judge > this.judge) return 1;
            else {
                if (o.score < this.score) return 1;
                else if (o.score > this.score) return -1;
                else
                    return 0;
            }
        }
    }


    private InfoNeed findNeed(List<InfoNeed> infoNeedList, int qID) {
        for (InfoNeed need : infoNeedList)
            if (need.id() == qID)
                return need;
        throw new RuntimeException("cannot find query ID " + qID + " in the list!");
    }

    /**
     * Traverse runs folder, and pick judged documents in the result list (top 1000)
     *
     * @param track web track
     * @throws IOException
     */
    private void scoreDistOverQRels(Track track) throws IOException {


        final Path runs_path = dataSet.collectionPath().resolve("runs").resolve(tag).resolve(track.toString());


        List<Path> fileList = Files.walk(runs_path)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());


        final Path score_path = dataSet.collectionPath().resolve("score_dist").resolve(tag).resolve(track.toString());

        if (!Files.exists(score_path))
            Files.createDirectories(score_path);


        for (Path p : fileList) {


            Map<Integer, List<Triple>> map = new LinkedHashMap<>();

            if (Files.isHidden(p)) continue;

            Path f = p.getFileName();
            // System.out.println(f.toString());

            PrintWriter out = new PrintWriter(Files.newBufferedWriter(score_path.resolve(f), StandardCharsets.US_ASCII));

            List<String> lines = Files.readAllLines(p, StandardCharsets.US_ASCII);

            for (String line : lines) {
                String[] parts = whiteSpaceSplitter.split(line);


                double score = Double.parseDouble(parts[4]);

                int qID = Integer.parseInt(parts[0]);

                String docId = parts[2];

                if (DOC_ID_LENGTH != docId.length() && DOC_ID_LENGTH + 1 != docId.length())
                    throw new RuntimeException("invalid doc id : " + docId);

                InfoNeed need = findNeed(track.getTopics(), qID);

                int judge = need.getJudge(docId);

                if (-5 == judge) continue;

                // System.out.println(qID + " " + judge + " " + score);

                if (map.containsKey(qID)) {
                    List<Triple> list = map.get(qID);
                    list.add(new Triple(judge, docId, score));
                } else {
                    List<Triple> list = new ArrayList<>();
                    list.add(new Triple(judge, docId, score));
                    map.put(qID, list);
                }
            }

            for (Map.Entry<Integer, List<Triple>> entry : map.entrySet()) {

                int qID = entry.getKey();

                List<Triple> list = entry.getValue();

                Collections.sort(list);

                out.print("qID:");
                out.print(qID);
                out.print("\t");

                for (Triple triple : list) {
                    out.print(triple.score);
                    out.print("\t");
                }

                out.println();

                out.print("qID:");
                out.print(qID);
                out.print("\t");

                for (Triple triple : list) {
                    out.print(triple.judge);
                    out.print("\t");
                }

                out.println();
            }

            out.flush();
            out.close();
        }


    }

    /**
     * Save TermFreqDist over QRels (judged (non)relevant) documents information to a single file in an exportable fashion.
     * Save query relevance judgement levels too.
     *
     * @throws IOException
     */
    public void saveDistOverQRels(List<InfoNeed> needs, Sheet sheet) throws IOException {


        int r = 0;
        for (InfoNeed need : needs) {

            if (need.termCount() > 1) continue;
            final String term = Analyzers.getAnalyzedToken(need.query());

            List<Integer> set = new ArrayList<>(new HashSet<>(need.getJudgeMap().values()));
            Collections.sort(set);
            Collections.reverse(set);

            List<Triple> list = new ArrayList<>();
            for (int judge : set) {

                // keys are docIDs, values are relative frequencies
                Map<String, Double> m = getRelativeFreqList(need, "contents", judge, term);

                for (Map.Entry<String, Double> entry : m.entrySet()) {
                    list.add(new Triple(judge, entry.getKey(), entry.getValue()));
                }
            }

            Collections.sort(list);

            System.out.println(need.id() + "\t" + set);
            Row r1 = sheet.createRow(r++);
            Row r2 = sheet.createRow(r++);

            r1.createCell(0, CellType.STRING).setCellValue(term);
            r2.createCell(0, CellType.STRING).setCellValue(term);

            for (int i = 0; i < list.size(); i++) {
                Triple triple = list.get(i);
                r1.createCell(i + 1, CellType.NUMERIC).setCellValue(triple.score);
                r2.createCell(i + 1, CellType.NUMERIC).setCellValue(triple.judge);
            }

            set.clear();
            list.clear();
        }
    }


    /**
     * @param map   query relevance map
     * @param judge given level
     * @return list of document ids that judged with given level
     */
    public static List<String> convert(Map<String, Integer> map, int judge) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == judge)
                list.add(entry.getKey());
        }
        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }


}
