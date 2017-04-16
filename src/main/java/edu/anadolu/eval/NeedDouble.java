package edu.anadolu.eval;

import org.apache.commons.math3.util.Precision;
import org.clueweb09.InfoNeed;

/**
 * To hold information need - phraseness pairs.
 * Useful for sorting information needs descending by document frequency ratio
 */
public final class NeedDouble implements Comparable<NeedDouble> {

    public final InfoNeed need;
    public final Double phraseness;

    public NeedDouble(InfoNeed need, double d) {
        this.need = need;
        this.phraseness = d;
    }

    @Override
    public int compareTo(NeedDouble o) {
        return this.phraseness.compareTo(o.phraseness);
    }

    @Override
    public String toString() {
        return need.toString() + "(" + Precision.round(phraseness, 2) + ")";
    }
}
