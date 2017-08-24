package edu.anadolu.eval;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * To hold model-score pairs.
 * Useful for sorting term-weighting models descending by their effectiveness
 */
final public class ModelScore implements Comparable<ModelScore> {

    final public String model;
    final public double score;

    public ModelScore(String model, double score) {
        this.model = model;
        this.score = score;
    }

    public double score() {
        return score;
    }
    public String model() {
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModelScore that = (ModelScore) o;

        if (Double.compare(that.score, score) != 0) return false;
        return model != null ? model.equals(that.model) : that.model == null;

    }

    @Override
    public int compareTo(ModelScore o) {
        if (equals(o)) return 0;

        final int doubleCompare = Double.compare(o.score, score);

        if (doubleCompare == 0)
            return model.compareTo(o.model);
        else
            return doubleCompare;
    }

    @Override
    public String toString() {
        return model + "(" + score + ")";
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + (model == null ? 0 : model.hashCode());
        hashCode = 31 * hashCode + Double.hashCode(score);
        return hashCode;
    }

    public static void main(String[] args) {
        TreeSet<ModelScore> set = new TreeSet<>();
        set.add(new ModelScore("bm25", 0.6));
        set.add(new ModelScore("dfi", 0.6));
        set.add(new ModelScore("lgd", 0.6));
        System.out.println(set);
    }
}
