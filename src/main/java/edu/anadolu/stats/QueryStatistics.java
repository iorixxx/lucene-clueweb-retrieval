package edu.anadolu.stats;

import edu.anadolu.datasets.DataSet;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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

    public QueryStatistics(DataSet dataset) {
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
     */

    public void saveLaTexStats() throws IOException {

        PrintWriter out = new PrintWriter(Files.newBufferedWriter(dataset.collectionPath().resolve("stats").resolve("query_stats.tex"), StandardCharsets.US_ASCII));

        for (Track track : dataset.tracks()) {
            List<InfoNeed> needs = track.getTopics();
            printStats(track, needs, out);
        }

        printStats(null, dataset.getTopics(), out);

        out.flush();
        out.close();


    }

    private void printStats(Track track, List<InfoNeed> needs, PrintWriter out) {

        DescriptiveStatistics spam = new DescriptiveStatistics();
        DescriptiveStatistics relevant = new DescriptiveStatistics();
        DescriptiveStatistics nonRelevant = new DescriptiveStatistics();

        int queryLength = 0;

        for (InfoNeed need : needs) {
            queryLength += need.wordCount();
            relevant.addValue(need.relevant());
            nonRelevant.addValue(need.nonRelevant());
            spam.addValue(need.spam());
        }

        out.print(track == null ? "ALL" : track.toString());
        out.print(" & ");

        out.print(needs.size());
        out.print(" & ");

        out.print(String.format("%.1f", (double) queryLength / needs.size()));
        out.print(" & ");

        out.print(averageAndStandardDeviation(relevant));
        out.print(" & ");

        out.print(averageAndStandardDeviation(spam));
        out.print(" & ");

        out.print(averageAndStandardDeviation(nonRelevant));
        out.print(" & ");

        out.print(track == null ? -1 : track.getJudgeLevels().size());

        out.print("\\\\");
        out.println();

        System.out.println((track == null ? "ALL" : track.toString()) + " s=" + spam.getSum() + " r=" + relevant.getSum());
    }

    /**
     * Always report the mean (average value) along with a measure of variablility (standard deviation(s) or standard error of the mean ).
     * <p>
     * Display average value along with plus-minus \pm standard deviation inside parenthesis
     *
     * @return pretty string
     */
    private String averageAndStandardDeviation(DescriptiveStatistics stats) {
        return String.format("%.1f ($\\pm$ %.1f)", stats.getMean(), stats.getStandardDeviation());
    }
}
