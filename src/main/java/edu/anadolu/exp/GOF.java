package edu.anadolu.exp;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.commons.math3.distribution.PoissonDistribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GOF2Poisson
 */
public class GOF extends ScorpioExperiment {

    public GOF(String tag, DataSet dataSet) {
        super(null, tag, dataSet);
    }

    Map<Integer, Long> observedFrequencies(String term) throws IOException {
        Path p = Paths.get(dataSet.collectionPath().toString(), "verbose_freqs", tag, term);

        List<String> lines = Files.readAllLines(p, StandardCharsets.US_ASCII);

        Map<Integer, Long> observedFrequencies = new HashMap<>();

        observedFrequencies.put(0, numberOfDocuments - termStatsMap.get(term).docFreq());

        for (String line : lines) {
            if (line.startsWith("value")) continue;


            try {
                String[] parts = line.split("\t");
                long count = Long.parseLong(parts[1]);
                int value = Integer.parseInt(parts[0]);

                observedFrequencies.put(value, count);

            } catch (java.lang.NumberFormatException nfe) {
                System.out.println(p.toString());
            }

        }


        return observedFrequencies;
    }

    public void gof2Poisson(String t) throws IOException {

        String term = Analyzers.getAnalyzedToken(t, analyzer);

        Map<Integer, Long> observedFrequencies = observedFrequencies(term);

        double lambda = (double) termStatsMap.get(term).totalTermFreq() / numberOfDocuments;


        System.out.println("lambda = " + lambda);

        PoissonDistribution poisson = new PoissonDistribution(lambda);

        for (int tf = 0; tf < 20; tf++) {

            double prob = poisson.probability(tf);
            long observed = observedFrequencies.get(tf);

            long expected = Math.round(prob * numberOfDocuments);

            long observedMinusExpected = observed - expected;

            double chi = (double) observedMinusExpected * observedMinusExpected / expected;
            System.out.println(tf + "\t& " + observed + "\t& " + String.format("%.4f", prob) + "\t& " + expected + "\t& " + observedMinusExpected + "\t& " + String.format("%.2f", chi) + "\\\\");
        }
    }
}
