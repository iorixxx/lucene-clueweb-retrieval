package edu.anadolu.freq;

/**
 * Raw Term Frequencies are normalized by field length
 * to create grouped frequency distribution
 */
public class LengthNormalized implements BinningStrategy {

    final int numOfBins;

    public LengthNormalized(int numOfBins) {
        this.numOfBins = numOfBins;
    }

    protected int bin(double percentage) {

        final int value = (int) (percentage * numOfBins);

        if (value == numOfBins)
            return value;
        else
            return value + 1;
    }


    @Override
    public int calculateBinValue(double percentage) {

        if (!(percentage >= 0 && percentage <= 1))
            throw new RuntimeException("percentage is out of range exception, percentage = " + percentage);

        final int result = bin(percentage);

        // bin must be between 1 and number of bins e.g. [1 .. numOfBins]
        if (result >= 1 && result <= numOfBins)
            return result;
        else
            throw new RuntimeException("bin is out of range exception, bin = " + result);


    }

    @Override
    public int numBins() {
        return numOfBins;
    }

    @Override
    public String toString() {
        return "length normalized term frequencies are radix count into " + numOfBins + " bins";
    }
}
