package edu.anadolu.eval;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * handles output of gdeval.pl
 * <p>
 * runid,topic,ndcg@20,err@20
 */
public final class GdEval implements EvalTool {

    Map<Integer, Element> elementList = new HashMap<>();

    private class Element {
        final String runid;
        final String topic;
        private final String ndcg;
        private final String err;


        public Element(String line) {

            int i = line.lastIndexOf(",");
            if (i == -1) throw new IllegalArgumentException("unexpected formatted line : " + line);
            err = line.substring(i + 1);

            int j = line.lastIndexOf(",", i - 1);
            if (j == -1) throw new IllegalArgumentException("unexpected formatted line : " + line);
            ndcg = line.substring(j + 1, i);


            int k = line.lastIndexOf(",", j - 1);
            if (k == -1) throw new IllegalArgumentException("unexpected formatted line : " + line);
            topic = line.substring(k + 1, j);

            runid = line.substring(0, k);
        }

        @Override
        public String toString() {
            return "runid=" + runid + System.getProperty("line.separator") +
                    "topic=" + topic + System.getProperty("line.separator") +
                    "ndcg=" + ndcg + System.getProperty("line.separator") +
                    "err=" + err;
        }

    }

    public GdEval(Path path) throws IOException {

        List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

        for (String line : lines) {
            line = line.trim();
            Element element = new Element(line);

            if ("topic".equals(element.topic) || "amean".equals(element.topic))
                continue;

            elementList.put(Integer.parseInt(element.topic), element);
        }
        lines.clear();
    }

    @Override
    public String getMetric(InfoNeed need, Metric metric) {

        if (!elementList.containsKey(need.id())) {
           /** if (202 == need.id() || 225 == need.id()) **/ return "0.00000";
           // throw new RuntimeException("need : " + need + " cannot be found!");
        }

        Element element = elementList.get(need.id());

        if (Metric.MAP.equals(metric))
            throw new UnsupportedOperationException("MAP metric is not reported by gdeval utility");
        else if (Metric.P.equals(metric))
            throw new UnsupportedOperationException("P metric is not reported by gdeval utility");
        else if (Metric.ERR.equals(metric))
            return element.err;
        else if (Metric.NDCG.equals(metric))
            return element.ndcg;
        else
            throw new AssertionError(this);
    }
}