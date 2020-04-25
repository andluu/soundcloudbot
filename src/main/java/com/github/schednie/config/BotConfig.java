package com.github.schednie.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;


@Getter
@Component
public class BotConfig {

    @Value("${BOT_TOKEN}")
    private String botToken;
    @Value("${BOT_USERNAME}")
    private String botUsername;

    // Endpoints
    @Value("${TRACKS_ENDPOINT}${CLIENT_ID}")
    private String tracksEndpoint;
    @Value("${STREAM_ENDPOINT}${CLIENT_ID}")
    private String streamEndpoint;
    @Value("${METADATA_ENDPOINT}${CLIENT_ID}")
    private String metadataEndpoint;

    // Text patterns
    @Value("${BUTTON_CALLDATA_TMPAGEIDX}")
    private String buttonCallDataTmpPageIdxText;
    @Value("${MESSAGE_HELP}")
    private String messageHelpText;
    @Value("${MESSAGE_FINDTRACKSNOTHING}")
    private String messageNoTracksFound;
    @Value("${MESSAGE_FINDTRACKSERROR}")
    private String messageFindError;
    @Value("${MESSAGE_TRACKDOWNLOADERROR}")
    private String messageDownloadError;
    @Value("${MESSAGE_TRACKMENUNOTEXIST}")
    private String messageMenuDoesNotExist;
    @Value("${MESSAGE_TRACK}")
    private String messageTrack;
    @Value("${BUTTON_TEXT_CURRPAGEIDX}")
    private String buttonTextCurrPageIdx;

    private static final String LINK_REGEX = "(https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?";
    public static final Pattern LINK_PATTERN = Pattern.compile(LINK_REGEX);

}
