package edu.anadolu.exp;

import org.apache.commons.math3.util.Precision;

/**
 * Helper to hold string - double pairs, useful for soring strings by some double key
 */
public final class StringDoublePair implements Comparable<StringDoublePair> {

    final String string;
    final double d;

    public StringDoublePair(String string, double d) {
        this.string = string;
        this.d = d;
    }

    @Override
    public String toString() {
        return string + "(" + Precision.round(d, 4) + ")";
    }

    @Override
    public int compareTo(StringDoublePair o) {
        if (o.d < this.d) return 1;
        else if (o.d > this.d) return -1;
        else return 0;
    }
}
