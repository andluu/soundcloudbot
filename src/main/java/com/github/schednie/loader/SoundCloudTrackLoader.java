package com.github.schednie.loader;

import com.github.schednie.model.Track;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface SoundCloudTrackLoader {

    /**
     * Finds tracks with its metadata according to input find query
     * @param findQuery find query
     * @param size count of tracks to find
     * @return list of tracks metadata, e.g. without respective File property
     */
    List<Track> findTracks(String findQuery, int size) throws URISyntaxException, IOException;

    /**
     * Downloads track from given link
     * @param url link to the track
     * @return instance of Track with downloaded track file
     */
    Track download(String url) throws IOException;
}
