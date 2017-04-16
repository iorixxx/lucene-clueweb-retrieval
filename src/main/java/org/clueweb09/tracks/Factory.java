package org.clueweb09.tracks;

import org.clueweb09.InfoNeed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Factory for Track implementations and their InfoNeed
 */
public class Factory {

    public static Track[] WTs(String home) {
        return new Track[]{
                new WT09(home),
                new WT10(home),
                new WT11(home),
                new WT12(home),
                new WT13(home),
                new WT14(home)
        };
    }

    public static Track[] TTs(String home) {
        return new Track[]{
                new TT04(home),
                new TT05(home),
                new TT06(home)
        };
    }


    public static void fetchQueries() throws IOException {
        final String[] urls = {
                "http://trec.nist.gov/data/web/09/wt09.topics.queries-only",
                "http://trec.nist.gov/data/web/10/wt2010-topics.queries-only",
                "http://trec.nist.gov/data/web/11/queries.101-150.txt",
                "http://trec.nist.gov/data/web/12/queries.151-200.txt"
        };


        for (String urlStr : urls) {

            URL url = new URL(urlStr);

            System.out.println();

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    System.out.println("\"" + inputLine + "\",");
            }

        }

    }

    public static void display(Track track) {
        List<InfoNeed> needs = track.getTopics();
        System.out.println(track.toString() + " Needs Size = " + needs.size());
        for (InfoNeed need : track.getTopics())
            System.out.println(need);
        System.out.println("===================");
    }

    public static void main(String[] args) throws IOException {

        String home = "/Users/iorixxx/TFD_HOME";

        for (Track track : Factory.WTs(home)) {
            display(track);
        }

        MQ09 mq09 = new MQ09(home);
        display(mq09);

        for (Track track : Factory.TTs(home)) {
            display(track);
        }

        ROB04 rob04 = new ROB04(home);
        display(rob04);
    }
}
