package edu.anadolu.exp;

/**
 * Hepler class for  Scorpio Experiment
 */
public final class PairScore implements Comparable<PairScore> {
    final int count1;
    final int count2;
    final double score;

    PairScore(int count1, int count2, double score) {
        this.count1 = count1;
        this.count2 = count2;
        this.score = score;
    }

    @Override
    public int compareTo(PairScore o) {
        if (this.score > o.score) return -1;
        else if (this.score < o.score) return 1;
        else
            return 0;
    }

    @Override
    public String toString() {
        return count1 + " , " + count2;
    }

    public String toString(String t1, String t2) {
        return t1 + "(" + count1 + ")," + t2 + "(" + count2 + ")";
    }
}
