package com.github.schednie.loader;


import com.github.schednie.model.Track;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SoundCloudTrackLoaderImpl implements SoundCloudTrackLoader {

    private static final String PLAYLIST_REGEX = "^(?:https:\\/\\/|http:\\/\\/|www\\.|)soundcloud\\.com\\/(:.+)\\/sets\\/(:.+)$";
    private static final String CLIENT_ID_API = "a3e059563d7fd3372b49b37f00a00bcf";
    /**
     * format: trackUrl
     */
    private static final String METADATA = "https://api.soundcloud.com/resolve.json?url=%s&client_id=" + CLIENT_ID_API;
    /**
     * format: trackNumber
     */
    private static final String STREAM = "https://api.soundcloud.com/tracks/%s/stream?client_id=" + CLIENT_ID_API;

    /**
     * format: findQuery, limit
     */
    private static final String TRACKS = "http://api.soundcloud.com/tracks?q=%s&limit=%d&client_id=" + CLIENT_ID_API;

    private static final Logger LOG = LoggerFactory.getLogger(SoundCloudTrackLoaderImpl.class);

    @Override
    public List<Track> findTracks(String findQuery, int count) throws URISyntaxException, IOException {
        LOG.info("Searching for {} tracks by query={}", count, findQuery);

        JSONArray jsonTracksArray = new JSONArray(IOUtils.toString(
                new URI(String.format(TRACKS, encodeValue(findQuery), count)), StandardCharsets.UTF_8));

        List<Track> foundTracks = new ArrayList<>(count);
        jsonTracksArray.forEach(o -> {
            JSONObject jsonTrack = (JSONObject) o;
            foundTracks.add(Track.builder()
                    .id(jsonTrack.getLong("id"))
                    .performer(jsonTrack.getJSONObject("user").getString("username"))
                    .title(jsonTrack.getString("title"))
                    .url(jsonTrack.getString("permalink_url"))
                    .build()
            );
        });
        return foundTracks;
    }

    @Override
    public Track download(String trackUrl) throws IOException {
        Track trackMetadata = parseTrackMetadata(trackUrl);

        LOG.info("Downloading track: {}", trackMetadata);

        Path target = Paths.get(trackMetadata.getPerformer() + " - " + trackMetadata.getTitle() + ".mp3");
        Files.copy(new URL(String.format(STREAM, trackMetadata.getId())).openStream(), target);
        File trackFile = target.toFile();

        LOG.debug("{}", trackFile);

        trackMetadata.setFile(trackFile);

        return trackMetadata;
    }

    private static Track parseTrackMetadata(String trackUrl) throws IOException {
        if (!trackUrl.startsWith("https://soundcloud.com")) throw new IllegalArgumentException(trackUrl);

        LOG.debug("Parsing track metadata, trackUrl: " + trackUrl);

        JSONObject jsonObject = new JSONObject(IOUtils.toString(
                new URL(String.format(METADATA, trackUrl)), StandardCharsets.UTF_8));

        long id = jsonObject.getLong("id");
        String performer = jsonObject.getJSONObject("user").getString("username");
        String title = jsonObject.getString("title");

        return Track.builder()
                .url(trackUrl)
                .performer(performer)
                .id(id)
                .title(title)
                .build();
    }


    private static String encodeValue(String value) {
        try {
            LOG.trace("Encoding value='{}'", value);
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
            LOG.debug("Encoded value='{}'", encodedValue);
            return encodedValue;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Failed to encode value=" + value, ex);
        }
    }

}
