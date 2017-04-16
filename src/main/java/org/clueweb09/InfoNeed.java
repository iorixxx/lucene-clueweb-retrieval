package org.clueweb09;

import edu.anadolu.datasets.DataSet;
import org.clueweb09.tracks.Track;

import java.util.*;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Encapsulates an information need (called topic in TREC jargon)
 */
public class InfoNeed {

    private final int relevant;

    public int relevant() {
        return relevant;
    }

    private final int spam;

    public int spam() {
        return spam;
    }

    private final int id;
    private final String query;
    private final String[] partialQuery;

    public final Set<String> distinctSet;

    public String query() {
        return query;
    }

    public int id() {
        return id;
    }

    /**
     * Query Length
     *
     * @return number of words in the query
     */

    public int wordCount() {
        return partialQuery.length;
    }

    /**
     * Unique Term Count
     *
     * @return number of unique terms
     */
    public int termCount() {
        return distinctSet.size();
    }


    public String getDistinctQuery() {
        StringBuilder builder = new StringBuilder();
        for (String s : distinctSet)
            builder.append(s).append(" ");
        return builder.toString().trim();
    }

    public Track getWT() {
        return track;
    }

    private final Track track;

    private int subTopicCount = 0;
    private String type = "";

    public void setType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public void setSubTopicCount(int subTopicCount) {
        this.subTopicCount = subTopicCount;
    }

    private final Map<String, Integer> judgeMap;

    public Map<String, Integer> getJudgeMap() {
        return judgeMap;
    }

    private final int nonRelevant;

    public int nonRelevant() {
        return nonRelevant;
    }

    private DataSet dataSet = null;

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public DataSet dataSet() {
        return this.dataSet;
    }

    public InfoNeed(InfoNeed need) {
        this.judgeMap = Collections.unmodifiableMap(need.judgeMap);
        this.nonRelevant = need.nonRelevant;
        this.relevant = need.relevant;
        this.track = need.track;
        this.spam = need.spam;
        this.query = need.query;
        this.id = need.id;
        this.partialQuery = need.partialQuery;
        this.type = need.type;
        this.subTopicCount = need.subTopicCount;
        this.distinctSet = need.distinctSet;
        this.dataSet = need.dataSet;
    }

    public InfoNeed(int id, String query, Track track, Map<String, Integer> judgeMap) {
        this.id = id;
        this.query = query;
        this.partialQuery = whiteSpaceSplitter.split(query);
        this.track = track;
        this.judgeMap = Collections.unmodifiableMap(judgeMap);

        int numRelevant = 0;
        int numNonRelevant = 0;
        int numSpam = 0;

        for (int judgeLevel : judgeMap.values()) {
            if (judgeLevel > 0) numRelevant++;
            else numNonRelevant++;

            if (judgeLevel == -2) numSpam++;
        }

        this.relevant = numRelevant;
        this.nonRelevant = numNonRelevant;
        this.spam = numSpam;


        LinkedHashSet<String> distinctSet = new LinkedHashSet<>();
        distinctSet.addAll(Arrays.asList(partialQuery));

        this.distinctSet = Collections.unmodifiableSet(distinctSet);
    }

    public int getJudge(String docID) {

        if (judgeMap.containsKey(docID)) {
            return judgeMap.get(docID);
        } else
            return -5;  // un-judged
    }

    public String getPartOfQuery(int k) {

        if (k == 0) return query;

        if (k - 1 < partialQuery.length)
            return partialQuery[k - 1];

        return null;

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InfoNeed)) return false;
        InfoNeed other = (InfoNeed) o;
        return other.id == this.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return id + ":" + query;
    }
}
