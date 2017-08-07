package edu.anadolu.stats;

import edu.anadolu.datasets.DataSet;
import org.apache.commons.math3.stat.StatUtils;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Salient Query Statistics
 */
public final class QueryStatistics {

    private final DataSet dataset;

    public QueryStatistics(DataSet dataset) throws IOException {
        this.dataset = dataset;
    }

    public void queryLengthHistogram() {

        int[] counts = new int[20];
        Arrays.fill(counts, 0);

        for (Track track : dataset.tracks()) {

            List<InfoNeed> needs = track.getTopics();

            for (InfoNeed need : needs) {
                counts[need.wordCount()]++;
            }
        }


        for (int i = 1; i < counts.length; i++)
            System.out.println(i + "\t" + counts[i]);
    }


    /**
     * Save query statistics to a LaTex file
     *
     * @throws IOException
     */

    public void saveLaTexStats() throws IOException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(dataset.collectionPath().resolve("stats").resolve("query_stats.tex"), StandardCharsets.US_ASCII));


        for (Track track : dataset.tracks()) {


            List<InfoNeed> needs = track.getTopics();

            double[] relevant = new double[needs.size()];
            double[] nonRelevant = new double[needs.size()];

            int queryLength = 0;
            int counter = 0;
            for (InfoNeed need : needs) {

                queryLength += need.wordCount();
                nonRelevant[counter] = need.nonRelevant();
                relevant[counter] = need.relevant();
                counter++;

            }

            out.print(track.toString());
            out.print(" & ");

            out.print(needs.size());
            out.print(" & ");

            out.print(String.format("%.1f", (double) queryLength / needs.size()));
            out.print(" & ");

            out.print(averageAndStandardDeviation(relevant));
            out.print(" & ");

            out.print(averageAndStandardDeviation(nonRelevant));
            out.print(" & ");

            out.print(track.getJudgeLevels().size());


            out.print("\\\\");
            out.println();
        }

        out.flush();
        out.close();

    }

    /**
     * Display average value along with plus-minus \pm standard deviation inside parenthesis
     *
     * @param list integer items
     * @return pretty string
     */
    private String averageAndStandardDeviation(double[] list) {
        return String.format("%.1f ($\\pm$ %.1f)", StatUtils.mean(list), Math.sqrt(StatUtils.variance(list)));
    }
}
