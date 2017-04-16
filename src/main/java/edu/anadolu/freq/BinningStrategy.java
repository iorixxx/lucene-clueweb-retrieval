package edu.anadolu.freq;

public interface BinningStrategy {
    int calculateBinValue(double relativeFrequency);

    int numBins();
}
