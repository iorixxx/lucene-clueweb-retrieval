package edu.anadolu.stats;

/**
 * Salient statistics for a query
 */
public final class QueryStats {

    public final long docCount;
    final long docLenAcc;
    final long docLenSquareAcc;

    public QueryStats(long docCount, long docLenAcc, long docLenSquareAcc) {
        this.docCount = docCount;
        this.docLenAcc = docLenAcc;
        this.docLenSquareAcc = docLenSquareAcc;
    }

    public double avdl() {
        return (double) docLenAcc / docCount;
    }

    public double variance() {
        return (docLenSquareAcc - (docLenAcc * docLenAcc / (docCount * 1d))) / (docCount - 1d);
    }

    public double meanDividedByVariance() {
        return avdl() / variance();
    }

    public double meanDividedBySE() {
        return avdl() / Math.sqrt(variance());
    }

    public double metric(String metric) {
        if ("df".equals(metric))
            return docCount;
        else if ("avdl".equals(metric))
            return avdl();
        else if ("vardl".equals(metric))
            return variance();
        else if ("meanvar".equals(metric))
            return meanDividedByVariance();
        else
            throw new AssertionError("use df, avdl, vardl, or meanvar. Unknown specificity metric " + metric);
    }
}
