package edu.anadolu.freq;

/**
 * Round two digits after zero
 */
public class Round2Binning implements BinningStrategy {
    @Override
    public int calculateBinValue(double relativeFrequency) {

        final String s = String.format("%.2f", relativeFrequency);

        if ("1.00".equals(s)) return 100;

        if (s.length() != 4) throw new RuntimeException("rounded string length is not five! " + s);
        if (s.indexOf(".") != 1) throw new RuntimeException("second character is not dot! " + s);
        if (!s.startsWith("0.")) throw new RuntimeException(s + " does not start with '0.'");

        return Integer.parseInt(s.substring(2));
    }

    @Override
    public int numBins() {
        return 2;
    }


    public static void main(String[] args) {

        Round2Binning binning = new Round2Binning();

        LengthNormalized normalized = new LengthNormalized(100);


        for (int i = 0; i < 10000; i++) {
            double d = Math.random();
            System.out.println(String.format("%.2f", d) + "\t" + binning.calculateBinValue(d) + "\t" + normalized.calculateBinValue(d));

            if (Math.abs(binning.calculateBinValue(d) - normalized.calculateBinValue(d)) > 1)
                throw new RuntimeException("difference of bins are greater than one!");
        }


    }
}