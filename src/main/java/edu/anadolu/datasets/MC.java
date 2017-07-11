package edu.anadolu.datasets;

import org.clueweb09.tracks.Track;

/**
 * Created by Gokhan on 11.07.2017.
 */
public class MC extends DataSet{
    public MC(String tfd_home){
        super(Collection.MC,new Track[]{
                new org.clueweb09.tracks.MC(tfd_home)
        },tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return null;
    }
}
