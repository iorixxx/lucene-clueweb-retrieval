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

    public boolean validateDocID(String docID) {
        if (null == docID) return false;
        else return !"".equals(docID.trim());
    }

    public abstract boolean spamAvailable();

    private final List<InfoNeed> needs;

    protected DataSet(Collection collection, Track[] tracks, String tfd_home) {
        this.collection = collection;
        this.tracks = tracks;
        this.tfd_home = tfd_home;
        List<InfoNeed> infoNeedList = new ArrayList<>();
        for (Track track : tracks()) {
            for (InfoNeed need : track.getTopics()) {
                need.setDataSet(this);
                infoNeedList.add(need);
            }
        }
        this.needs = Collections.unmodifiableList(infoNeedList);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSet dataSet = (DataSet) o;
        return collection == dataSet.collection &&
                Arrays.equals(tracks, dataSet.tracks) &&
                Objects.equals(tfd_home, dataSet.tfd_home) &&
                Objects.equals(needs, dataSet.needs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(collection);
        result = 31 * result + Arrays.hashCode(tracks);
        return result;
    }
}
