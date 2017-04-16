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
 * handles output of statAP_MQ_eval_v3.pl
 * <p>
 * topic=1  MIXED
 * Relevant=220.290989  sampled=246 sampled_relev=86
 * sampled_in_list=145  sampled_relev_in_list=85
 * AP=0.315661  R-prec=0.316174  Prec_at_30=0.133333  nDCG=0.0765437554960422
 * varAP=0.000267967353551343
 */
public final class StatAP implements EvalTool {

    Map<Integer, Element> elementList = new HashMap<>();

    private class Element {
        int topic;
        private String nDCG_10;
        private String nDCG_30;
        private String nDCG_50;
        private String nDCG_100;
        String ap;
        String p30;

        public Element(String paragraph) {

            paragraph = paragraph.trim().replaceAll("--no relevant docs", "");

            if (!paragraph.startsWith("topic="))
                throw new IllegalArgumentException("unexpected formatted line : " + paragraph);

            String[] parts = paragraph.split("\\s+");

            if (parts.length != 17 && parts.length != 18)
                throw new IllegalArgumentException("paragraph does not have 17 nor 18 entries : " + paragraph);

            for (String entry : parts) {

                if ("MIXED".equals(entry) || "NEU".equals(entry)) continue;

                String[] subParts = entry.split("=");

                if (subParts.length != 2)
                    throw new IllegalArgumentException("unexpected formatted entry : " + entry);

                if ("topic".equals(subParts[0])) topic = Integer.parseInt(subParts[1]);
                if ("nDCG_30".equals(subParts[0])) nDCG_30 = subParts[1];
                if ("nDCG_100".equals(subParts[0])) nDCG_100 = subParts[1];
                if ("AP".equals(subParts[0])) ap = subParts[1];
                if ("Prec_at_30".equals(subParts[0])) p30 = subParts[1];
            }

            if ("0".equals(nDCG_30)) nDCG_30 = "0.00000";
            if ("0".equals(nDCG_100)) nDCG_100 = "0.00000";
        }

        @Override
        public String toString() {
            return "topic=" + topic + System.getProperty("line.separator") +
                    "nDCG_30=" + nDCG_30 + System.getProperty("line.separator") +
                    "AP" + ap + System.getProperty("line.separator") +
                    "Prec_at_30" + p30;
        }

    }

    private final int k;

    public StatAP(Path path, int k) throws IOException {

        this.k = k;
        List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

        if (lines.get(0).trim().length() != 0)
            throw new IllegalArgumentException("In StatAP, first line should be empty : " + lines.get(0));


        StringBuilder buffer = new StringBuilder();

        // skip fist line
        for (int i = 1; i < lines.size(); i++) {

            final String line = lines.get(i).trim();
            // last paragraph reached
            if (line.startsWith("valid_topics=")) break;

            if (line.length() == 0 || line.startsWith("valid_topics=")) {

                Element element = new Element(buffer.toString().trim());
                elementList.put(element.topic, element);

                buffer.setLength(0);


                continue;
            }

            buffer.append(line).append(" ");
        }

        lines.clear();
    }


    @Override
    public String getMetric(InfoNeed need, Metric metric) {

        if (!elementList.containsKey(need.id()))
            throw new RuntimeException("need : " + need + " cannot be found!");

        Element element = elementList.get(need.id());

        if (Metric.MAP.equals(metric))
            return element.ap;


        if ((Metric.NDCG.equals(metric) || Metric.ERR.equals(metric)) && k == 20)
            return element.nDCG_30;

        if ((Metric.NDCG.equals(metric) || Metric.ERR.equals(metric)) && (k == 100 || k == 1000))
            return element.nDCG_100;

        throw new UnsupportedOperationException("Unsupported metric : " + metric + "@" + k);
    }
}
