package edu.anadolu.datasets;

import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Entity to represent a collection and its query set(s). e.g. MQ09 and ClueWeb09B
 */
public abstract class DataSet {

    protected final Collection collection;
    protected final Track[] tracks;
    protected final String tfd_home;

    public abstract String getNoDocumentsID();

    public abstract boolean spamAvailable();

    private final List<InfoNeed> needs;

    private final Map<String, Integer> judgeMap;

    protected DataSet(Collection collection, Track[] tracks, String tfd_home) {
        this.collection = collection;
        this.tracks = tracks;
        this.tfd_home = tfd_home;
        List<InfoNeed> infoNeedList = new ArrayList<>();
        Map<String, Integer> judgeMap = new HashMap<>();
        for (Track track : tracks()) {
            for (InfoNeed need : track.getTopics()) {
                need.setDataSet(this);
                infoNeedList.add(need);
                judgeMap.putAll(track.getMap().get(need.id()));
            }
        }
        this.needs = Collections.unmodifiableList(infoNeedList);
        this.judgeMap = Collections.unmodifiableMap(judgeMap);
    }

    public int judge(String docId) {
        return judgeMap.getOrDefault(docId, -5);
    }

    public Path collectionPath() {
        return Paths.get(tfd_home, collection.toString());
    }

    public Path home() {
        return Paths.get(tfd_home);
    }

    public Path indexesPath() {
        return Paths.get(tfd_home, collection.toString(), "indexes");
    }


    public Track[] tracks() {
        return tracks;
    }

    public Collection collection() {
        return collection;
    }

    public List<InfoNeed> getTopics() {
        return needs;
    }

    @Override
    public String toString() {
        return collection.toString() + ":" + Arrays.toString(tracks);
    }
}
