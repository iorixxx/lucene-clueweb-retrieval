package edu.anadolu.stats;

import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;

/**
 * Salient statistics for a term
 */
public final class TermStats extends TermStatistics {

    private final double cti;

    public TermStats(String term, long docFreq, long totalTermFreq, double cti) {
        super(new BytesRef(term), docFreq, totalTermFreq);
        this.cti = cti;
    }

    public final double cti() {
        return cti;
    }

    private double avdl;

    public void setAvdl(double avdl) {
        this.avdl = avdl;
    }

    public double avdl() {
        return this.avdl;
    }

    private double vardl;

    public void setVardl(double vardl) {
        this.vardl = vardl;
    }

    public double vardl() {
        return this.vardl;
    }

    public double meanDividedByVariance() {
        return avdl() / vardl();
    }

    public double meanDividedBySE() {
        return avdl() / Math.sqrt(vardl());
    }

    public double metric(String metric) {
        if ("tf".equals(metric))
            return totalTermFreq();
        else if ("df".equals(metric))
            return docFreq();
        else if ("cti".equals(metric))
            return cti();
        else if ("avdl".equals(metric))
            return avdl();
        else if ("vardl".equals(metric))
            return vardl();
        else if ("meanvar".equals(metric))
            return meanDividedByVariance();
        else
            throw new AssertionError("use tf, df, cti, avdl, vardl, or meanvar. Unknown specificity metric " + metric);
    }

}
