package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class MC extends Track {

    public MC(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws Exception {

        Path topicsPath = Paths.get(home, "topics-and-qrels", "queriesMC.csv");
        List<String> lines = Files.readAllLines(topicsPath, StandardCharsets.UTF_8);


        for (String line : lines) {

            String[] parts = line.split("\",\"");
            int qID = Integer.parseInt(parts[0].substring(1));
            String query = parts[1];

            if (!isJudged(qID)) {
                System.out.println(qID + ":" + query + " is not judged. Skipping...");
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

        lines.clear();

    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrelsMC.txt"));
    }

    public static void main(String[] args) {
        String line = "\"235\",\"Kuş Gribi\",\"Kuş gribi nedir, nasıl bulaşır, belirtileri nelerdir sorularına cevap olabilecek dokümanlar.\",\"Kuş gribi ile alakalı her türlü bilginin elde edilebileceği bir doküman olmalı. Hastalığının tanımını, bulaşma yollarını, belirtilerini, varsa tedavi yollarını ve buna benzer birçok konuyu okuyucuya açıklayan nitelikte bir doküman.\"";
        String[] parts = line.split("\",\"");

        System.out.println(parts[0].substring(1));
        System.out.println("--" + parts[1] + "--");
        for (String p : parts) {
            System.out.println(p);
        }

    }


}
