package edu.anadolu.freq;

import org.clueweb09.tracks.Track;

import java.io.IOException;
import java.nio.file.Path;

public interface TFD {
    void processSingeTrack(Track track, Path path) throws IOException;
}
