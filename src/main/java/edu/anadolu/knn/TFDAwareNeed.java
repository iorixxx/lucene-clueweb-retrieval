package edu.anadolu.knn;

import edu.anadolu.analysis.Analyzers;
import org.clueweb09.InfoNeed;

import java.util.*;

/**
 * Term Frequency Distribution Aware Information Need
 */
public class TFDAwareNeed extends InfoNeed {

    final List<Long[]> termFreqDist;
    List<Long[]> termFreqDistZero;
    public final List<Double[]> termFreqDistNormalized;

    public final Double[] termFreqDistNormalized(String word) {
        return termFreqDistMap.get(Analyzers.getAnalyzedToken(word));
    }


    final Double[] dfAndAverage;
    final Double[] averageAndDF;

    final Double[] geoAndDF;
    final Double[] dfAndGeo;

    final Long[] average;
    Long[] averageZero;

    private <T extends Number> void checkArraySizes(List<T[]> list) {
        final int size = list.get(0).length;

        for (T[] a : list)
            if (a.length != size)
                throw new RuntimeException("arrays in the list have different size than " + size);
    }

    public <T extends Number> TFDAwareNeed(InfoNeed need, List<T[]> termFreqDist) {
        super(need);
        checkArraySizes(termFreqDist);
        this.termFreqDist = new ArrayList<>(termFreqDist.size());

        for (T[] t : termFreqDist) {
            this.termFreqDist.add(number2long(t));
        }

        this.termFreqDistNormalized = normalize(termFreqDist);

        this.dfAndAverage = dfAndAverage(termFreqDist);
        this.averageAndDF = averageAndDF(termFreqDist);
        this.geoAndDF = null; //geoAndDF(termFreqDist);
        this.dfAndGeo = null;//dfAndGeo(termFreqDist);
        this.average = averageLong(termFreqDist);
    }

    LinkedHashMap<String, Double[]> termFreqDistMap;

    public TFDAwareNeed(InfoNeed need, LinkedHashMap<String, Long[]> termFreqDistMap) {
        this(need, new ArrayList<>(termFreqDistMap.values()));
        this.termFreqDistMap = map2map(termFreqDistMap);
        this.rawTermFreqDistMap = termFreqDistMap;
    }

    private <T extends Number> LinkedHashMap<String, Double[]> map2map(Map<String, T[]> termFreqDistMap) {
        LinkedHashMap<String, Double[]> map = new LinkedHashMap<>();

        for (Map.Entry<String, T[]> entry : termFreqDistMap.entrySet()) {
            T[] a = entry.getValue();
            double df = df(a);
            final Double[] array = new Double[a.length];
            for (int i = 0; i < a.length; i++)
                array[i] = a[i].doubleValue() / df;

            map.put(entry.getKey(), array);

        }

        return map;
    }


    public List<Double[]> termFreqDistZeroNormalized;

    public Double[] termFreqDistZeroNormalized(String word) {
        return termFreqDistZeroMap.get(Analyzers.getAnalyzedToken(word));
    }

    Double[] dfAndAverageZero;
    Double[] averageAndDFZero;

    Double[] geoAndDFZero;
    Double[] dfAndGeoZero;

    public Long[] termFreqDistZeroRaw(String word) {
        return rawTermFreqDistZeroMap.get(Analyzers.getAnalyzedToken(word));
    }

    LinkedHashMap<String, Long[]> rawTermFreqDistZeroMap = new LinkedHashMap<>();
    LinkedHashMap<String, Long[]> rawTermFreqDistMap = new LinkedHashMap<>();


    public <T extends Number> void setZero(List<T[]> termFreqDistZero) {
        checkArraySizes(termFreqDistZero);
        this.termFreqDistZero = new ArrayList<>(termFreqDistZero.size());
        for (T[] t : termFreqDistZero)
            this.termFreqDistZero.add(number2long(t));

        this.termFreqDistZeroNormalized = normalize(termFreqDistZero);
        this.dfAndAverageZero = dfAndAverage(termFreqDistZero);
        this.averageAndDFZero = averageAndDF(termFreqDistZero);
        this.geoAndDFZero = null;//geoAndDF(termFreqDistZero);
        this.dfAndGeoZero = null;//dfAndGeo(termFreqDistZero);

        this.averageZero = averageLong(termFreqDistZero);
    }

    LinkedHashMap<String, Double[]> termFreqDistZeroMap;

    public void setZero(LinkedHashMap<String, Long[]> termFreqDistZeroMap) {
        this.setZero(new ArrayList<>(termFreqDistZeroMap.values()));
        this.termFreqDistZeroMap = map2map(termFreqDistZeroMap);
        this.rawTermFreqDistZeroMap = termFreqDistZeroMap;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder(super.toString()).append("\n");
        builder.append("--------------------------\n");

        for (String word : distinctSet) {

            Long[] array = rawTermFreqDistZeroMap.get(Analyzers.getAnalyzedToken(word));

            builder.append(id()).append(":").append(word).append("\t");

            for (int i = 0; i < Math.min(10, array.length); i++)
                builder.append(array[i]).append("\t");

            builder.append("\n");
        }
        /**
         builder.append(id()).append(":").append("Geo\t");
         for (int i = 0; i < 10; i++)
         builder.append(String.format("%.1f", geo[i])).append("\t");
         builder.append("\n");


         builder.append(id()).append(":").append("Average\t");
         for (int i = 0; i < 10; i++)
         builder.append(String.format("%.1f", average[i])).append("\t");
         builder.append("\n");


         builder.append(id()).append(":").append("MetaTerm\t");
         for (int i = 0; i < 10; i++)
         builder.append(queryFreqDist[i]).append("\t");
         builder.append("\n");
         **/

        return builder.toString();
    }

    static <T extends Number> Double[] average(List<T[]> list) {

        if (list.size() == 1) return number2double(list.get(0));

        final Double[] array = new Double[list.get(0).length];
        Arrays.fill(array, 0.0);

        for (T[] a : list)
            for (int i = 0; i < array.length; i++)
                array[i] += a[i].doubleValue();


        for (int i = 0; i < array.length; i++)
            array[i] /= list.size();

        return array;

    }

    public static <T extends Number> Double[] dfAndAverage(List<T[]> list) {

        // if (list.size() == 1) return list.get(0);

        final Double[] array = new Double[list.get(0).length];
        Arrays.fill(array, 0.0);

        for (T[] a : list) {
            double df = df(a);
            for (int i = 0; i < array.length; i++)
                array[i] += a[i].doubleValue() / df;

        }

        for (int i = 0; i < array.length; i++)
            array[i] /= (double) list.size();

        return array;

    }

    public static Double[] dfAndGeo(List<Long[]> list) {

        List<Double[]> normalized = normalize(list);

        return geoD(normalized);

    }

    public static Long df(Long[] R) {
        long df = 0L;
        for (Long l : R)
            df += l;
        return df;
    }

    public static <T extends Number> Double df(T[] R) {
        double df = 0.0;
        for (T t : R)
            df += t.doubleValue();
        return df;
    }

    static <T extends Number> Double[] number2double(T[] array) {
        Double[] doubles = new Double[array.length];
        for (int i = 0; i < array.length; i++)
            doubles[i] = array[i].doubleValue();

        return doubles;
    }

    static <T extends Number> Long[] number2long(T[] array) {
        Long[] longs = new Long[array.length];
        for (int i = 0; i < array.length; i++)
            longs[i] = array[i].longValue();
        return longs;
    }

    public static <T extends Number> List<Double[]> normalize(List<T[]> termFreqDist) {

        List<Double[]> returnValue = new ArrayList<>(termFreqDist.size());

        for (T[] a : termFreqDist) {
            double df = df(a);
            final Double[] array = new Double[a.length];
            for (int i = 0; i < a.length; i++)
                array[i] = a[i].doubleValue() / df;

            returnValue.add(array);

        }

        return returnValue;

    }

    public static <T extends Number> Double[] averageAndDF(List<T[]> list) {

        Double[] average = average(list);

        Double df = df(average);

        final Double[] array = new Double[list.get(0).length];
        Arrays.fill(array, 0.0);

        for (int i = 0; i < average.length; i++)
            array[i] = (double) average[i] / df;

        return array;
    }

    public static Double[] geoAndDF(List<Long[]> list) {

        Double[] geo = geo(list);

        Double df = df(geo);

        final Double[] array = new Double[list.get(0).length];
        Arrays.fill(array, 0.0);

        for (int i = 0; i < geo.length; i++)
            array[i] = (double) geo[i] / df;

        return array;
    }


    static Double[] geo(List<Long[]> list) {

        if (list.size() == 1) return number2double(list.get(0));

        final Double[] result = new Double[list.get(0).length];
        Arrays.fill(result, 0.0);


        for (int i = 0; i < result.length; i++) {
            double mul = 1.0;
            for (Long[] a : list)
                mul *= a[i];

            result[i] = Math.pow(mul, 1.0d / list.size());
        }


        return result;


    }

    static Double[] geoD(List<Double[]> list) {

        if (list.size() == 1) return list.get(0);

        final Double[] result = new Double[list.get(0).length];
        Arrays.fill(result, 0.0);


        for (int i = 0; i < result.length; i++) {
            double mul = 1.0;
            for (Double[] a : list)
                mul *= a[i];

            result[i] = Math.pow(mul, 1.0d / list.size());
        }


        return result;


    }

    static <T extends Number> Long[] averageLong(List<T[]> list) {

        final Long[] array = new Long[list.get(0).length];
        Arrays.fill(array, 0L);

        for (T[] a : list)
            for (int i = 0; i < array.length; i++)
                array[i] += a[i].longValue();


        for (int i = 0; i < array.length; i++)
            array[i] /= list.size();

        return array;

    }
}
