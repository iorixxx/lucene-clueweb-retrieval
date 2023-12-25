package edu.anadolu.eval;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * handles output of <a href="http://trec.nist.gov/trec_eval/trec_eval_latest.tar.gz">trec_eval</a>
 */
public final class TrecEval implements EvalTool {

    Map<Integer, Element> elementList = new HashMap<>();

    class Element {

        private int qID = -1;
        private String map = null;
        private String p = null;
        private String recall = null;
        private String ncg = null;
        final private int k;


        public Element(List<String> lines, int k) {

            this.k = k;

            for (String line : lines) {
                if (line.startsWith("map")) {
                   String[] parts = whiteSpaceSplitter.split(line);

                    if (parts.length != 3) throw new RuntimeException("line does not have 3 parts:  " + line);
                    map = parts[2];

                    qID = Integer.parseInt(parts[1]);

                }

                if (line.startsWith("P_" + Integer.toString(k))) {
                    String[] parts = whiteSpaceSplitter.split(line);

                    if (parts.length != 3) throw new RuntimeException("line does not have 3 parts:  " + line);
                    p = parts[2];
                    qID = Integer.parseInt(parts[1]);

                }
                if (line.startsWith("recall_" + Integer.toString(k))) {
                    String[] parts = whiteSpaceSplitter.split(line);

                    if (parts.length != 3) throw new RuntimeException("line does not have 3 parts:  " + line);
                    recall = parts[2];
                    qID = Integer.parseInt(parts[1]);

                }
                if (line.startsWith("ncg_cut_" + Integer.toString(k))) {
                    String[] parts = whiteSpaceSplitter.split(line);

                    if (parts.length != 3) throw new RuntimeException("line does not have 3 parts:  " + line);
                    ncg = parts[2];
                    qID = Integer.parseInt(parts[1]);

                }
            }

            if (p == null || qID == -1 || map == null)
                throw new RuntimeException("metrics cannot be extracted! " + lines);

        }


        @Override
        public String toString() {
            return "topic=" + qID + System.getProperty("line.separator") +
                    "map=" + map + System.getProperty("line.separator") +
                    "recall@" + k + "=" + recall + System.getProperty("line.separator") +
                    "ncg@" + k + "=" + ncg + System.getProperty("line.separator") +
                    "p@" + k + "=" + p;
        }
    }

    public TrecEval(Path path, int k) throws IOException {

        List<String> innerList = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);
        for (String line : lines) {

            line = line.trim();

            if (innerList.size() > 0 && (line.startsWith("num_ret") || line.startsWith("runid"))) {

                Element element = new Element(innerList, k);
                elementList.put(element.qID, element);
                innerList.clear();
            }

            if (line.startsWith("runid")) break;

            innerList.add(line);

        }

        lines.clear();
    }

    @Override
    public String getMetric(InfoNeed need, Metric metric) {

        if (!elementList.containsKey(need.id()))
            throw new RuntimeException("need : " + need + " cannot be found!");

        Element element = elementList.get(need.id());

        if (Metric.MAP.equals(metric))
            return element.map;
        else if (Metric.P.equals(metric))
            return element.p;
        else if (Metric.Recall.equals(metric))
            return element.recall;
        else if (Metric.NCG.equals(metric))
            return element.ncg;
        else if (Metric.ERR.equals(metric))
            throw new UnsupportedOperationException("ERR metric is not reported by trec_eval utility");
        else if (Metric.NDCG.equals(metric))
            throw new UnsupportedOperationException("NDCG metric is not reported by trec_eval utility");
        else
            throw new AssertionError(this);
    }
}

