package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.analysis.Analyzer;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Query Bank
 */
public final class QueryBank {

    private final List<InfoNeed> ALL_QUERIES;

    public Set<String> distinctTerms(Analyzer analyzer) {
        Set<String> set = new HashSet<>();
        for (InfoNeed need : ALL_QUERIES)
            set.addAll(Analyzers.getAnalyzedTokens(need.query(), analyzer));
        return Collections.unmodifiableSet(set);
    }

    public QueryBank(DataSet dataSet) {

        ALL_QUERIES = new ArrayList<>();
        for (Track track : dataSet.tracks())
            ALL_QUERIES.addAll(track.getTopics());

    }

    public List<InfoNeed> getAllQueries(int minRelevantDocs) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.relevant() > minRelevantDocs)
                        .collect(Collectors.toList()
                        )
        );
    }

    public List<InfoNeed> getTwoTermQueries(int minRelevantDocs) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.relevant() > minRelevantDocs)
                        .filter(need -> need.wordCount() == 2)
                        .collect(Collectors.toList()
                        )
        );
    }

    public List<InfoNeed> getQueries(int minRelevantDocs, int wordCount) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.relevant() > minRelevantDocs)
                        .filter(need -> need.wordCount() == wordCount)
                        .collect(Collectors.toList()
                        )
        );
    }

    public List<InfoNeed> getOneTermQueries(int minRelevantDocs) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.relevant() > minRelevantDocs)
                        .filter(need -> need.wordCount() == 1)
                        .collect(Collectors.toList()
                        )
        );
    }

    public List<InfoNeed> getTrainingQueries(Track track) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.getWT() != track)
                        .collect(Collectors.toList()
                        )
        );
    }

    public List<InfoNeed> getTestQueries(Track track) {
        return Collections.unmodifiableList(
                ALL_QUERIES.stream()
                        .filter(need -> need.getWT() == track)
                        .collect(Collectors.toList()
                        )
        );
    }
}
