package com.github.schednie.loader;

import com.github.schednie.config.BotConfig;
import com.github.schednie.model.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {BotConfig.class})
@TestPropertySource("classpath:config.properties")
public class SoundCloudTrackLoaderImplTest {

    private SoundCloudTrackLoaderImpl loader;

    public SoundCloudTrackLoaderImplTest(final BotConfig botConfig) {
        this.loader = new SoundCloudTrackLoaderImpl(botConfig);
    }

    private static final int TRACK_COUNT = 50;

    @Test
    public void findTracks_latinQueryGiven_realTracksMetadataReturned() throws IOException, URISyntaxException {
        String findQuery = "nombe wait";
        List<Track> someRealTracks = nombeRealTrackList();

        List<Track> foundTracks = loader.findTracks(findQuery, TRACK_COUNT);

        assertTrue(foundTracks.size() <= TRACK_COUNT);
        assertNotEquals(foundTracks.size(), 0);
        assertTrue(foundTracks.containsAll(someRealTracks));
    }

    @Test
    public void findTracks_cyrillicQueryGiven_realTracksMetadataReturned() throws IOException, URISyntaxException {
        String findQuery = "хиккан";
        List<Track> someRealTracks = xikkanRealTrackList();

        List<Track> foundTracks = loader.findTracks(findQuery, TRACK_COUNT);

        assertTrue(foundTracks.size() <= TRACK_COUNT);
        assertNotEquals(foundTracks.size(), 0);
        assertTrue(foundTracks.containsAll(someRealTracks));
    }

    @Test
    public void download_correctLinkGiven_correctDownloadedTrackReturned() throws IOException {
        String correctTrackLink = "https://soundcloud.com/nombe/california-girls";
        String title = "California Girls";
        String performer = "NoMBe";
        int bytes = 3472403;

        Track downloadedTrack = loader.download(correctTrackLink);

        assertEquals(downloadedTrack.getFile().length(), bytes);
        assertEquals(downloadedTrack.getTitle(), title);
        assertEquals(downloadedTrack.getPerformer(), performer);
        assertEquals(downloadedTrack.getUrl(), correctTrackLink);
        assertEquals(downloadedTrack.getFile().getName(), performer + " - " + title + ".mp3");
        downloadedTrack.getFile().delete();
    }

    @Test
    public void download_trackFromPlaylist_correctTrack() throws IOException {
        String trackFromPlaylistUrl = "https://soundcloud.com/tortured_soul/tortured-soul-fall-in-love-1?in=lovely-1986/sets/acid-jazz-funky-mix";

        Track downloadedTrack = loader.download(trackFromPlaylistUrl);

        assertEquals(downloadedTrack.getTitle(), "Tortured Soul - Fall In Love (Eric Kupper Remix)");
        assertEquals(downloadedTrack.getPerformer(), "Tortured_Soul");
        assertEquals(downloadedTrack.getUrl(), trackFromPlaylistUrl);
        downloadedTrack.getFile().delete();
    }

    private static List<Track> xikkanRealTrackList() {
        return Arrays.asList(
                Track.builder().performer("Xikkan#1").title("ХИККАН №1 - МЫ ВСЁ ПРОЕБАЛИ").url("https://soundcloud.com/xikkan-1/1a-1").build(),
                Track.builder().performer("Xikkan#1").title("ХИККАН №1 - ЛИЦО КАВАЙНОЙ НАЦИОНАЛЬНОСТИ").url("https://soundcloud.com/xikkan-1/1a-8").build()
        );
    }

    private static List<Track> nombeRealTrackList() {
        return Arrays.asList(
                Track.builder().performer("NoMBe").title("Wait").url("https://soundcloud.com/nombe/wait").build(),
                Track.builder().performer("NoMBe").title("Wait (Kill Them With Colour Remix").url("https://soundcloud.com/nombe/wait-kill-them-with-colour").build()
        );
    }
}