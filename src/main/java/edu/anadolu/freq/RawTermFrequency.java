package edu.anadolu.freq;

/**
 * Raw Term Frequency
 */
public class RawTermFrequency implements BinningStrategy {
    @Override
    public int calculateBinValue(double relativeFrequency) {
        return (int) relativeFrequency;
    }

    @Override
    public int numBins() {
        return 0;
    }

    @Override
    public String toString() {
        return "raw term frequencies are counted";
    }
}
