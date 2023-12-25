package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.stats.QueryStats;
import edu.anadolu.stats.TermStats;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.analysis.Analyzer;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Utility to select subset of queries according to some criteria,
 * e.g. bring me queries that have a common term in them.
 */
public class QuerySelector {

    private final String tag;
    private final Path collectionPath;
    public final Map<String, TermStats> termStatisticsMap;
    private final Map<InfoNeed, QueryStats> queryStatsMap;

    public final long numberOfDocuments;

    public final long numberOfTokens;

    public final List<InfoNeed> allQueries;
    private final DataSet dataSet;
    protected final Analyzer analyzer;

    public Analyzer analyzer() {
        return this.analyzer;
    }


    public QuerySelector(DataSet dataSet, String tag) {
        this.dataSet = dataSet;

        allQueries = new ArrayList<>();

        for (Track track : dataSet.tracks()) {
            allQueries.addAll(track.getTopics());
        }

        this.tag = tag;
        this.analyzer = Analyzers.analyzer(Tag.tag(tag));
        this.collectionPath = dataSet.collectionPath();
        this.termStatisticsMap = loadTermStatsMap();
        this.queryStatsMap = loadQueryStatsMap();

        String line = Evaluator.loadCorpusStats(collectionPath, "contents", tag);
        String[] parts = whiteSpaceSplitter.split(line);

        if (parts.length != 4)
            throw new RuntimeException("line from field_stats.csv does not have four parts " + line);

        numberOfDocuments = Long.parseLong(parts[2]);
        numberOfTokens = Long.parseLong(parts[1]);


    }

    public LinkedHashMap<String, String> getFrequencyDistributionList(InfoNeed need, String fileName) {

        Path freqPath = collectionPath.resolve("freqs");
        Track track = need.getWT();

        Path path = freqPath.resolve(tag).resolve(track.toString()).resolve(fileName);

        final List<String> lines = readAllLines(path);

        Set<String> analyzedTokens = new HashSet<>(Analyzers.getAnalyzedTokens(need.query(), analyzer));

        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        for (String word : analyzedTokens)
            for (String line : lines)
                if (line.startsWith(need.id() + ":" + word + "\t")) {
                    map.put(word, line);
                    break;
                }

        if (map.size() == analyzedTokens.size()) return map;
        else {
            System.out.println("analyzedTokens " + analyzedTokens);
            System.out.println("return list " + map.size());
            throw new RuntimeException(need.toString() + " not found in " + path.toString());
        }

    }

    /**
     * Construct a descriptive statistics object for a given frequency line
     *
     * @param line frequency line
     * @return DescriptiveStatistics
     */
    public DescriptiveStatistics toDescriptiveStatistics(String line) {

        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();

        final String[] parts = whiteSpaceSplitter.split(line);

        if (parts[1].contains("stopword")) {
            System.err.println("Returning NaN for " + line);
            descriptiveStatistics.addValue(0);
            return descriptiveStatistics;
        }

        for (int i = 1; i < parts.length; i++) {
            try {
                descriptiveStatistics.addValue(Double.parseDouble(parts[i]));
            }catch (Exception ex){
                descriptiveStatistics.addValue(0);
            }
        }
        return descriptiveStatistics;
    }

    private List<Long> toLong(String line) {

        final String[] parts = whiteSpaceSplitter.split(line);
        List<Long> list = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            list.add(Long.parseLong(parts[i]));
        }
        return list;
    }

    private int maxBinIndex(String line) {

        List<Long> list = toLong(line);

        double max = Collections.max(list);

        for (int i = 0; i < list.size(); i++)
            if (list.get(i) == max) return i;

        return -1;

    }

    /**
     * Maximum break-even point
     *
     * @param need information need
     * @return integer value of climax point
     */
    public int maxBin(InfoNeed need) {

        int max = Integer.MIN_VALUE;

        for (String line : getFrequencyDistributionList(need, "contents_all_freq_1000.csv").values()) {

            int i = maxBinIndex(line);

            if (i > 0.5) System.out.println(line);

            if (i > max) max = i;
        }

        return max;
    }

    /**
     * Three - Four - Five term queries that do not have stop words in it
     *
     * @param e expected value
     * @return list of information needs
     */
    public List<InfoNeed> remaining(double e) {

        List<InfoNeed> needs = new ArrayList<>();
        for (InfoNeed need : allQueries) {
            if (need.wordCount() < 3) continue;
            if (isLambdaGreaterThan(need, e)) continue;
            needs.add(need);
        }
        return needs;
    }


    public List<InfoNeed> nonDecreasingQueries(double e) {

        List<InfoNeed> needs = new ArrayList<>();
        for (InfoNeed need : allQueries) {
            int maxBin = maxBin(need);
            if (maxBin > e) {
                needs.add(need);
            }
        }
        return needs;
    }

    public List<InfoNeed> lambdaQueries(double e) {

        List<InfoNeed> needs = new ArrayList<>();

        for (InfoNeed need : allQueries)
            if (isLambdaGreaterThan(need, e))
                if (!needs.contains(need))
                    needs.add(need);

        return needs;
    }

    public double maxLambda(InfoNeed need) throws IOException {

        double max = Double.NEGATIVE_INFINITY;

        for (String s : Analyzers.getAnalyzedTokens(need.query(), analyzer)) {
            double e = expectedUnderDBar(s);
            if (e > max)
                max = e;
        }

        return max;

    }

    public double expectedUnderDBar(String term) {
        return (double) termStatisticsMap.get(term).totalTermFreq() / (double) numberOfDocuments;
    }

    public boolean isLambdaGreaterThan(InfoNeed need, double e) {
        for (String s : Analyzers.getAnalyzedTokens(need.query(), analyzer))
            if (expectedUnderDBar(s) > e)
                return true;
        return false;
    }

    /**
     * calculate discriminative ratio (max/min) of the query terms
     *
     * @param need   information need
     * @param metric tf, df, cti, avdl, vardl, meanvar
     * @return discriminative ratio (max/min)
     */
    public double termRatio(InfoNeed need, String metric) {

        List<String> analyzedTokens = Analyzers.getAnalyzedTokens(need.query(), analyzer);

        if (analyzedTokens.size() == 1) {
            return termStatisticsMap.get(analyzedTokens.get(0)).metric(metric);
        }

        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for (String s : Analyzers.getAnalyzedTokens(need.query(), analyzer)) {

            double d = termStatisticsMap.get(s).metric(metric);

            if (d > max) max = d;
            if (d < min) min = d;
        }

        return max / min;
    }

    public double queryMetric(InfoNeed need, String metric) {
        return queryStatsMap.get(need).metric(metric);
    }

    /**
     * maximum of the metric among query terms
     *
     * @param need   information need
     * @param metric tf, df, cti, avdl, vardl, meanvar
     * @throws IOException
     */
    public double max(InfoNeed need, String metric) throws IOException {

        double max = Double.NEGATIVE_INFINITY;

        for (String s : Analyzers.getAnalyzedTokens(need.query(), analyzer)) {

            double l = termStatisticsMap.get(s).metric(metric);

            if (l > max) max = l;

        }

        return max;
    }


    public Map<String, TermStats> loadTermStatsMap() {
//        System.out.println("Loading term stats map for "+tag);
        Map<String, TermStats> termStatsMap = new HashMap<>();

        Path fieldStats = Paths.get(collectionPath.toString(), "stats", tag, "contents_term_stats.csv");
//        System.out.println(fieldStats.toString());
        List<String> lines = readAllLines(fieldStats);
        for (String line : lines) {
            if ("term \t totalTermFreq \t docFreq \t cti".equals(line)) continue;

            String[] parts = whiteSpaceSplitter.split(line);

            if (parts.length != 4)
                throw new RuntimeException("line from contents_term_stats.csv does not have four parts " + line);

            long totalTermFreq = Long.parseLong(parts[1]);
            long docFreq = Long.parseLong(parts[2]);
            double cti = Double.parseDouble(parts[3]);

            termStatsMap.put(parts[0], new TermStats(parts[0], docFreq, totalTermFreq, cti));

        }

        enrichTermStatsMap(termStatsMap);
        lines.clear();
        return Collections.unmodifiableMap(termStatsMap);
    }

    public Map<InfoNeed, QueryStats> loadQueryStatsMap() {

        Map<InfoNeed, QueryStats> queryStatsMap = new HashMap<>();

        Path fieldStats = Paths.get(collectionPath.toString(), "freqs", "QueryDocLength" + tag + ".csv");

        List<String> lines = readAllLines(fieldStats);

        for (String line : lines) {
            if ("qID\tN\tdocLenAcc\tdocLenSquareAcc".equals(line)) continue;

            String[] parts = whiteSpaceSplitter.split(line);

            if (parts.length != 4)
                throw new RuntimeException("line from QueryDocLength" + tag + ".csv" + " does not have four parts " + line);

            int qID = Integer.parseInt(parts[0]);

            for (InfoNeed need : allQueries) {
                if (qID == need.id()) {
                    queryStatsMap.put(need, new QueryStats(Long.parseLong(parts[1]), Long.parseLong(parts[2]), Long.parseLong(parts[3])));
                    break;
                }
            }
        }

        lines.clear();
        return Collections.unmodifiableMap(queryStatsMap);
    }

    private List<String> readAllLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Augment term stats map with document length statistics
     *
     * @param termStatsMap map loaded with basic stats
     */
    private void enrichTermStatsMap(Map<String, TermStats> termStatsMap) {

        Path documentStatsFile = Paths.get(collectionPath.toString(), "stats", tag, "contents_document_length_stats.csv");
//        System.out.println("Enriching term stats map for "+ tag);
//        System.out.println(documentStatsFile.toString());
        if (!Files.exists(documentStatsFile) || !Files.isRegularFile(documentStatsFile) || !Files.isReadable(documentStatsFile)) {
            System.out.println("Document length stats  file : " + documentStatsFile + " does not exist or is not a (readable) file.");
            return;
        }

        List<String> lines = readAllLines(documentStatsFile);

        for (String line : lines) {
            if ("Term\tN\tdocLenAcc\tdocLenSquareAcc".equals(line)) continue;

            String[] parts = whiteSpaceSplitter.split(line);

            //TODO add meaningful statistics for non-existing terms. e.g., docFreq = totalTermFreq = 0
            if (parts.length != 4) {
                System.out.println(collectionPath.toString() + " " + tag + ": While enriching termstatmap, parts is not equal to 4 for line: " + line);
                continue;
            }
            //throw new RuntimeException("line from contents_document_length_stats.csv does not have four parts " + line);


            String term = parts[0];

            if (!termStatsMap.containsKey(term))
                throw new RuntimeException("terms stats map does not contain term : " + term);

            TermStats termStats = termStatsMap.get(term);

            long N = Long.parseLong(parts[1]);
            long docLenAcc = Long.parseLong(parts[2]);
            long docLenSquareAcc = Long.parseLong(parts[3]);

            termStats.setAvdl((double) docLenAcc / N);

            double variance = (docLenSquareAcc - (double) (docLenAcc * docLenAcc / N)) / (N - 1);

            termStats.setVardl(variance);
        }

        lines.clear();
    }
}
