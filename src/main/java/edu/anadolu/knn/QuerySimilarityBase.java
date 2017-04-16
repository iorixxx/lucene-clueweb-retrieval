package edu.anadolu.knn;

import java.util.ArrayList;
import java.util.List;

import static edu.anadolu.knn.CartesianQueryTermSimilarity.Entry;

/**
 * Base class for query similarity implementations
 */
public abstract class QuerySimilarityBase implements QuerySimilarity {

    protected final ChiBase chi;
    protected final boolean zero;

    public QuerySimilarityBase(ChiBase chi, boolean zero) {
        this.chi = chi;
        this.zero = zero;
    }

    @Override
    public final ChiBase chi() {
        return chi;
    }

    @Override
    public final boolean zero() {
        return zero;
    }

    @Override
    public final String toString() {
        /** if (zero)
         return "Z," + name() + chi.toString();
         else
         **/
        return name(); // + chi.toString();
    }

    protected final void sanityCheck(TFDAwareNeed R) {
        if (R.termCount() != R.termFreqDistZeroNormalized.size())
            throw new RuntimeException("term count and zero freq dist list sizes are not same!");

        if (R.termCount() != R.termFreqDistNormalized.size())
            throw new RuntimeException("term count and freq dist list sizes are not same!");

        if (R.termFreqDistZeroNormalized.size() != R.termFreqDistNormalized.size())
            throw new RuntimeException("freq dist list sizes are not same!");
    }

    protected <T extends Number> List<Entry> entryList(List<T[]> R, List<T[]> S) {

        List<Entry> list = new ArrayList<>();

        int i = 0;
        for (T[] r : R) {

            int j = 0;
            for (T[] s : S) {
                list.add(new Entry(i, j, chi.chiSquared(r, s)));
                j++;
            }
            i++;
        }
        return list;
    }
}
