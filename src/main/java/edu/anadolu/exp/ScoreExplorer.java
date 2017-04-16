package edu.anadolu.exp;

import static org.apache.lucene.search.similarities.ModelBase.log2;

public class ScoreExplorer {


    protected static double lgd(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double numberOfDocuments) {
        final double tfn = tf * log2(1.0d + (averageDocumentLength) / docLength);
        double lambda = (1.0D * documentFrequency) / (1.0D * numberOfDocuments);
        return keyFrequency * log2((lambda + tfn) / lambda);
    }

    protected static double dph(double tf, long docLength, double averageDocumentLength, double keyFrequency, double termFrequency, double numberOfDocuments) {

        double f = tf < docLength ? tf / docLength : 0.9999;

        double norm = (1d - f) * (1d - f) / (tf + 1d);

        return keyFrequency * norm
                * (tf * log2((tf *
                averageDocumentLength / docLength) *
                (numberOfDocuments / termFrequency))
                + 0.5d * log2(2d * Math.PI * tf * (1d - f))
        );
    }

    protected static double dfi(double tf, long docLength, double keyFrequency, double termFrequency, double numberOfTokens) {
        double e_ij = (termFrequency * docLength) / numberOfTokens;

        // Condition 1
        if (tf <= e_ij) return 0D;

        double chiSquare = (Math.pow((tf - e_ij), 2) / e_ij) + 1;

        return keyFrequency * log2(chiSquare);
    }

    public static void main(String[] args) {

        System.out.println(Integer.MAX_VALUE);

        for (int i = 1; i <= 10; i++) {
            System.out.println("tf: " + i + " dph: " + dph(i, 10, 5, 1, 500, 1400));

        }


        for (int i = 1; i <= 10; i++) {
            System.out.println("tf: " + i + " lgd: " + lgd(i, 10, 10, 1, 100, 200));

        }

        for (int i = 1; i <= 10; i++) {
            System.out.println("tf: " + i + " dfi: " + dfi(i, 20, 1, 100, 1000));

        }


    }
}
