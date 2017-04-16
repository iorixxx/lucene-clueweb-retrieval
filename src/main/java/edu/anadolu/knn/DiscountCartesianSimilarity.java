package edu.anadolu.knn;

import org.paukov.combinatorics3.Generator;

import java.util.*;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Variant of CartesianQueryTermSimilarity that ignores same terms in queries
 */
public class DiscountCartesianSimilarity extends CartesianQueryTermSimilarity {

    public DiscountCartesianSimilarity(ChiBase chi, boolean zero, Aggregation agg, Way way) {
        super(chi, zero, agg, way);
    }

    /**
     * @param R Test Query
     * @param S Training Query
     * @return similarity
     */

    @Override
    public final double score(TFDAwareNeed R, TFDAwareNeed S) {

        sanityCheck(R);
        sanityCheck(S);

        /**
         * If one of them is a one-term query, then call super.
         */
        if (R.termCount() == 1 || S.termCount() == 1)
            return super.score(R, S);


        LinkedHashMap<String, Double[]> RMap = R.termFreqDistMap;
        LinkedHashMap<String, Double[]> SMap = S.termFreqDistMap;

        Set<String> intersection = new HashSet<>(RMap.keySet());
        intersection.retainAll(SMap.keySet());

        if (intersection.isEmpty()) return super.score(R, S);

        if (zero)
            return scoreX(discount(R.termFreqDistZeroMap, intersection), discount(S.termFreqDistZeroMap, intersection));
        else
            return scoreX(discount(R.termFreqDistMap, intersection), discount(S.termFreqDistMap, intersection));
    }

    protected <T extends Number> double couple(List<T[]> R, List<T[]> S) {

        if (R.size() != S.size()) throw new RuntimeException("sizes are not equal!");

        List<Entry> list = entryList(R, S);

        System.out.println("initial = " + list);
        Collections.sort(list);
        System.out.println("initial = " + list);


        final double[] values = new double[R.size()];

        for (int i = 0; i < R.size(); i++) {
            Entry entry = list.remove(0);
            System.out.println("removed = " + entry);
            values[i] = entry.similarity;
            remove(list, entry.i, entry.j);
            System.out.println(list);
        }

        if (!list.isEmpty()) throw new RuntimeException("list is not empty from couple similarity!");
        return aggregate(values);

    }

    private List<Double[]> discount(LinkedHashMap<String, Double[]> map, Set<String> intersection) {

        List<Double[]> list = new ArrayList<>();

        for (Map.Entry<String, Double[]> entry : map.entrySet()) {
            if (!intersection.contains(entry.getKey()))
                list.add(entry.getValue());
        }
        return list;
    }

    @Override
    public String name() {
        return "D" + aggregation();
    }

    public static void main(String[] args) {

        String r = "internet phone service";
        String s = "disneyland hotel";

        List<String> R = Arrays.asList(whiteSpaceSplitter.split(r));

        List<String> S = Arrays.asList(whiteSpaceSplitter.split(s));

        List<String> max = R.size() > S.size() ? R : S;

        List<String> min = R.size() < S.size() ? R : S;

        Generator.combination(max)
                .simple(min.size())
                .stream()
                .forEach(System.out::println);
    }
}
