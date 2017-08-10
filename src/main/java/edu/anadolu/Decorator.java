package edu.anadolu;

import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.Freq;
import edu.anadolu.freq.LengthNormalized;
import edu.anadolu.knn.TFDAwareNeed;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Decorates given information needs with frequency distribution information
 */
public final class Decorator extends QuerySelector {

    private final Map<Integer, TFDAwareNeed> cache;

    private final Freq type;
    private final int numBins;

    public Decorator(DataSet dataset, String tag, Freq type) throws IOException {
        this(dataset, tag, type, 1000);
    }

    public Decorator(DataSet dataset, String tag, Freq type, int numBins) throws IOException {
        super(dataset, tag);
        this.type = type;
        this.numBins = numBins;
        this.cache = residualTFDAwareNeeds(allQueries, type.fileName(numBins));
    }

    public String type() {
        return type.toString() + Integer.toString(numBins);
    }

    public int numBins() {
        return numBins;
    }

    public Collection<TFDAwareNeed> residualTFDAwareNeeds() throws IOException {
        return cache.values();
    }

    public Map<String, Set<TFDAwareNeed>> decorate(Map<String, List<InfoNeed>> map) {

        Map<String, Set<TFDAwareNeed>> decorated = new HashMap<>(map.size());

        for (Map.Entry<String, List<InfoNeed>> entry : map.entrySet()) {
            Set<TFDAwareNeed> needs = new HashSet<>(entry.getValue().size());
            for (InfoNeed need : entry.getValue()) {
                TFDAwareNeed tfdAwareNeed = tfdAwareNeed(need);
                if (needs.contains(tfdAwareNeed)) throw new RuntimeException("set shouldn't contain : " + need);
                else
                    needs.add(tfdAwareNeed);
            }
            decorated.put(entry.getKey(), needs);

        }

        return decorated;
    }

    public TFDAwareNeed tfdAwareNeed(int qID) {
        if (cache.containsKey(qID))
            return cache.get(qID);
        else throw new RuntimeException("cache does not contain query ID " + qID);
    }

    public TFDAwareNeed tfdAwareNeed(InfoNeed need) {
        if (cache.containsKey(need.id()))
            return cache.get(need.id());
        else throw new RuntimeException("cache does not contain " + need.toString());
    }

    public List<TFDAwareNeed> residualTFDAwareNeeds(List<InfoNeed> residualNeeds) throws IOException {
        return residualNeeds.stream().map(this::tfdAwareNeed).collect(Collectors.toList());
    }

    private static Double[] sqrt(Long[] array) {

        Double[] doubles = new Double[array.length];

        for (int i = 0; i < array.length; i++)
            doubles[i] = Math.sqrt(array[i]);

        return doubles;
    }

    public static List<Double[]> sqrt(List<Long[]> termFreqDist) {

        List<Double[]> returnValue = new ArrayList<>(termFreqDist.size());

        for (Long[] a : termFreqDist) {
            returnValue.add(sqrt(a));
        }
        return returnValue;
    }

    private static Double[] log(Long[] array) {

        Double[] doubles = new Double[array.length];

        for (int i = 0; i < array.length; i++)
            doubles[i] = Math.log(array[i] + 1.0);

        return doubles;
    }

    static Long[] cdf(Long[] R) {

        Long[] ret = new Long[R.length];

        Long cdf = 0L;
        for (int i = 0; i < R.length; i++) {
            cdf += R[i];
            ret[i] = cdf;
        }

        return ret;
    }

    private static Double[] ratio(Long[] junk) {

        Long[] cdf = cdf(junk);
        Double[] doubles = new Double[cdf.length - 1];

        for (int i = 1; i < cdf.length; i++) {
            doubles[i - 1] = (double) cdf[i] / cdf[i - 1];
        }

        return doubles;
    }

    public static List<Double[]> ratio(List<Long[]> termFreqDist) {
        return termFreqDist.stream().map(Decorator::ratio).collect(Collectors.toList());
    }

    public static void main(String[] args) {

        Long[] array = {97694L, 593L, 631L, 342L, 191L, 123L, 82L, 59L, 43L, 34L, 27L, 22L, 18L, 15L, 13L, 11L};
        Long[] array2 = {98037L, 700L, 468L, 232L, 129L, 91L, 63L, 44L, 35L, 28L, 23L, 19L, 15L, 14L, 11L, 9L};

        ArrayList<Long[]> list = new ArrayList<>();
        list.add(array);


        for (Double d[] : ratio(list))
            System.out.println(Arrays.toString(d));

        ArrayList<Long[]> list2 = new ArrayList<>();
        list2.add(array2);


        for (Double d[] : ratio(list2))
            System.out.println(Arrays.toString(d));

        LengthNormalized binning = new LengthNormalized(1000);

        double min = 0.001;
        double max = 0.5;
        int old = 1;
        System.out.println("0.001" + " " + binning.calculateBinValue(0.001));
        System.out.println("0.998" + " " + binning.calculateBinValue(0.998));
        System.out.println("0.999" + " " + binning.calculateBinValue(0.999));
        System.out.println("1.0" + " " + binning.calculateBinValue(1));
        for (double i = min; i <= max; i = i + 0.001) {

            System.out.println(i + " " + binning.calculateBinValue(i));
            double rebase = (i - min) / (max - min);
            //System.out.println(old++ + " " + binning.calculateBinValue(rebase));
        }
    }

    public static List<Double[]> log(List<Long[]> termFreqDist) {

        return termFreqDist.stream().map(Decorator::log).collect(Collectors.toList());

//        List<Double[]> returnValue = new ArrayList<>(termFreqDist.size());
//
//        for (Long[] a : termFreqDist) {
//            returnValue.add(log(a));
//        }
//        return returnValue;
    }

    private Map<Integer, TFDAwareNeed> residualTFDAwareNeeds(List<InfoNeed> residualNeeds, String fileName) throws IOException {

        Map<Integer, TFDAwareNeed> residualTFDAwareNeeds = new LinkedHashMap<>(residualNeeds.size());

        for (InfoNeed need : residualNeeds) {
            List<Long[]> termFreqDist = new ArrayList<>();
            List<Long[]> termFreqDistZero = new ArrayList<>();

            LinkedHashMap<String, Long[]> termFreqDistZeroMap = new LinkedHashMap<>();
            LinkedHashMap<String, Long[]> termFreqDistMap = new LinkedHashMap<>();

            LinkedHashMap<String, String> map = getFrequencyDistributionList(need, fileName);

            for (Map.Entry<String, String> entry : map.entrySet()) {

                String term = entry.getKey();
                String line = entry.getValue();
                Long[] l = parseFreqLine(line);
                Long[] z = Freq.Zero.equals(type) ? l.clone() : addZeroColumnToLine(l);
                termFreqDist.add(l);
                termFreqDistMap.put(term, l);
                termFreqDistZero.add(z);
                termFreqDistZeroMap.put(term, z);
            }
            TFDAwareNeed tfdAwareNeed;

            if (Freq.Log.equals(type)) {
                tfdAwareNeed = new TFDAwareNeed(need, log(termFreqDist));
                tfdAwareNeed.setZero(log(termFreqDistZero));

            } else if (Freq.Sqrt.equals(type)) {

                tfdAwareNeed = new TFDAwareNeed(need, sqrt(termFreqDist));
                tfdAwareNeed.setZero(sqrt(termFreqDistZero));

            } else if (Freq.Ratio.equals(type)) {

                tfdAwareNeed = new TFDAwareNeed(need, ratio(termFreqDist));
                tfdAwareNeed.setZero(ratio(termFreqDistZero));

            } else {
                tfdAwareNeed = new TFDAwareNeed(need, termFreqDistMap);
                tfdAwareNeed.setZero(termFreqDistZeroMap);

            }
            /**
             else {
             tfdAwareNeed = new TFDAwareNeed(need, termFreqDist);
             tfdAwareNeed.setZero(termFreqDistZero);
             }**/

            residualTFDAwareNeeds.put(tfdAwareNeed.id(), tfdAwareNeed);
        }

        return Collections.unmodifiableMap(residualTFDAwareNeeds);
    }

    /**
     * Parse single line of frequency file
     *
     * @param line frequency line
     * @return long array
     */
    public Long[] parseFreqLine(String line) {

        final Long[] array = new Long[numBins + 1];
        Arrays.fill(array, 0L);

        final String[] parts = whiteSpaceSplitter.split(line);

        // TODO some query terms do not exist in the Collection
        if (parts[1].endsWith("(stopword)"))
            return array;

        for (int i = 1; i < parts.length; i++)
            array[i - 1] = Long.parseLong(parts[i]);

        return array;
    }

    public Long[] addZeroColumnToLine(Long[] input) {

        final long df = TFDAwareNeed.df(input);

        long zero = numberOfDocuments - df;
        if (zero < 0) {
            System.out.println("numberOfDocuments - df < 0 " + zero);
            return insertZerothPosition(input, 0);
        } else
            return insertZerothPosition(input, numberOfDocuments - df);
    }

    static Long[] insertZerothPosition(Long[] input, long key) {

        final Long[] array = new Long[input.length];
        Arrays.fill(array, 0L);

        System.arraycopy(input, 0, array, 1, input.length - 1);
        array[0] = key;
        return array;

    }

    static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
