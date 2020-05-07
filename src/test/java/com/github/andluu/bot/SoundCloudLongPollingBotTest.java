package com.github.andluu.bot;

import com.github.andluu.config.BotConfig;
import com.github.andluu.loader.SoundCloudTrackLoaderImpl;
import com.github.andluu.model.Track;
import com.github.andluu.model.TrackMenu;
import com.github.andluu.repositories.TrackMenuRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.andluu.bot.SoundCloudLongPollingBot.TRACK_MENU_PAGE_COUNT;
import static com.github.andluu.bot.SoundCloudLongPollingBot.TRACK_MENU_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {BotConfig.class})
@TestPropertySource("classpath:config.properties")
public class SoundCloudLongPollingBotTest {

    private static final long CHAT_ID = 1337L;
    private static final int MESSAGE_ID = 42;

    private static List<Track> someTrackList;
    private static File testMp3;
    private SoundCloudTrackLoaderImpl loader = mock(SoundCloudTrackLoaderImpl.class);
    private TrackMenuRepository trackMenuRepository = mock(TrackMenuRepository.class);
    private SoundCloudLongPollingBot bot = mock(SoundCloudLongPollingBot.class);

    @BeforeAll
    public static void initTrackList() {
        testMp3 = new File(SoundCloudLongPollingBotTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        someTrackList = IntStream.range(0, 10).boxed().map((i) -> Track.builder()
                .id(1488)
                .performer(String.valueOf(i))
                .title(String.valueOf(i))
                .url("url" + i)
                .file(testMp3)
                .build()).collect(Collectors.toList());
    }

    @BeforeEach
    public void setUp() {
        doCallRealMethod().when(bot).onUpdateReceived(any());
        doCallRealMethod().when(bot).setTrackLoader(loader);
        doCallRealMethod().when(bot).setTrackMenuRepository(trackMenuRepository);
        bot.setTrackLoader(loader);
        bot.setTrackMenuRepository(trackMenuRepository);
    }

    @Test
    public void onUpdateReceived_correctLink_correctSendAudio() throws TelegramApiException, IOException {
        //GIVEN
        Track track = new Track(42, "NoMBe", "California Girls",
                "https://soundcloud.com/nombe/california-girls", testMp3);
        when(loader.download(track.getUrl())).thenReturn(track);

        //WHEN
        bot.onUpdateReceived(getUpdateWithMessage(track.getUrl()));

        //THEN
        ArgumentCaptor<SendAudio> argument = ArgumentCaptor.forClass(SendAudio.class);
        verify(bot, times(1)).send(argument.capture());
        SendAudio sendAudio = argument.getValue();
        assertEquals(sendAudio.getPerformer(), track.getPerformer());
        assertEquals(sendAudio.getTitle(), track.getTitle());
        assertEquals(sendAudio.getAudio().getNewMediaFile(), track.getFile());
        assertEquals(Long.valueOf(sendAudio.getChatId()).longValue(), CHAT_ID);
    }


    @Test
    public void onUpdateReceived_linkWithSomeText_correctSendAudio() throws TelegramApiException, IOException {
        //GIVEN
        Track track = new Track(42, "NoMBe", "California Girls",
                "https://soundcloud.com/nombe/california-girls", testMp3);
        when(loader.download(track.getUrl())).thenReturn(track);

        //WHEN
        bot.onUpdateReceived(getUpdateWithMessage(
                "Some text like if user share track" + track.getUrl()
                        + " directly from SoundCloud app"));

        //THEN
        ArgumentCaptor<SendAudio> argument = ArgumentCaptor.forClass(SendAudio.class);
        verify(bot, times(1)).send(argument.capture());
        SendAudio sendAudio = argument.getValue();
        assertEquals(sendAudio.getPerformer(), track.getPerformer());
        assertEquals(sendAudio.getTitle(), track.getTitle());
        assertEquals(sendAudio.getAudio().getNewMediaFile(), track.getFile());
        assertEquals(Long.valueOf(sendAudio.getChatId()).longValue(), CHAT_ID);
    }

    @Test
    public void onUpdateReceived_findQuery_correctTrackMenuButtonsMetadata() throws TelegramApiException, IOException, URISyntaxException {
        //GIVEN
        String findQuery = "some query";
        when(loader.findTracks(findQuery, TRACK_MENU_PAGE_SIZE * TRACK_MENU_PAGE_COUNT)).thenReturn(someTrackList);

        //WHEN
        bot.onUpdateReceived(getUpdateWithMessage(findQuery));

        //THEN
        ArgumentCaptor<SendMessage> argument = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(1)).send(argument.capture());
        SendMessage sendMessage = argument.getValue();
        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = replyMarkup.getKeyboard();
        List<InlineKeyboardButton> buttons = rows.get(0);
        assertEquals(rows.size(), 1); // rows count
        assertEquals(buttons.size(), TRACK_MENU_PAGE_COUNT); // buttons count
        assertTrue(IntStream.range(0, TRACK_MENU_PAGE_COUNT)
                .mapToObj(pageIdx -> new InlineKeyboardButton(pageIdx == 0 ?
                        String.format("* %d *", pageIdx) :
                        String.valueOf(pageIdx))
                        .setCallbackData(String.format("TM:%d", pageIdx)))
                .allMatch(buttons::contains)); // check buttons text and call data
        assertEquals(Long.valueOf(sendMessage.getChatId()).longValue(), CHAT_ID);
    }

    @ParameterizedTest
    @MethodSource("trackMenuPagesRange")
    public void onUpdateReceived_findQuery_correctTrackMenuText(int pageIdx) throws TelegramApiException, IOException, URISyntaxException {
        //GIVEN
        String findQuery = "some query";
        when(loader.findTracks(findQuery, TRACK_MENU_PAGE_SIZE * TRACK_MENU_PAGE_COUNT)).thenReturn(someTrackList);
        when(trackMenuRepository.findById(CHAT_ID)).thenReturn(Optional.of(new TrackMenu(CHAT_ID, someTrackList)));
        when(trackMenuRepository.existsById(CHAT_ID)).thenReturn(true);

        //WHEN
        bot.onUpdateReceived(getUpdateWithCallbackQuery(getPageButtonCallbackData(pageIdx))); // pick page #pageIdx

        //THEN
        ArgumentCaptor<EditMessageText> editMessageArgument = ArgumentCaptor.forClass(EditMessageText.class);
        verify(bot, times(1)).send(editMessageArgument.capture()); // capture edited message
        EditMessageText editMessage = editMessageArgument.getValue();
        String text = editMessage.getText();
        assertEquals(text, someTrackList.stream()
                .skip(pageIdx * TRACK_MENU_PAGE_SIZE)
                .limit(TRACK_MENU_PAGE_SIZE)
                .map(track -> String.format("/%d %s - %s \n",
                        someTrackList.indexOf(track), track.getPerformer(), track.getTitle()))
                .collect(Collectors.joining())); // Check text
        assertEquals(editMessage.getMessageId().intValue(), MESSAGE_ID); // edit performs on origin message
        assertEquals(Long.valueOf(editMessage.getChatId()).longValue(), CHAT_ID);
    }

    @ParameterizedTest
    @MethodSource("trackMenuPagesRange")
    public void onUpdateReceived_findQuery_correctTrackMenuButtonsHighlighting(int pageIdx) throws TelegramApiException, IOException, URISyntaxException {
        //GIVEN
        String findQuery = "some query";
        when(loader.findTracks(findQuery, TRACK_MENU_PAGE_SIZE * TRACK_MENU_PAGE_COUNT)).thenReturn(someTrackList);
        when(trackMenuRepository.findById(CHAT_ID)).thenReturn(Optional.of(new TrackMenu(CHAT_ID, someTrackList)));
        when(trackMenuRepository.existsById(CHAT_ID)).thenReturn(true);

        //WHEN
        bot.onUpdateReceived(getUpdateWithCallbackQuery(getPageButtonCallbackData(pageIdx))); // pick page #pageIdx

        //THEN
        ArgumentCaptor<EditMessageText> editMessageArgument = ArgumentCaptor.forClass(EditMessageText.class);
        verify(bot, times(1)).send(editMessageArgument.capture()); // capture edited message
        EditMessageText editMessage = editMessageArgument.getValue();
        List<InlineKeyboardButton> buttons = editMessageArgument.getValue().getReplyMarkup().getKeyboard().get(0);
        InlineKeyboardButton currPageBtn = buttons.get(pageIdx);
        // Check if only the curr page button is highlighted
        assertEquals(currPageBtn.getText(), String.format("* %d *", pageIdx));
        assertTrue(buttons.stream().filter(btn -> btn != currPageBtn).noneMatch(btn -> btn.getText().contains("*")));
        assertEquals(editMessage.getMessageId().intValue(), MESSAGE_ID); // edit performs on origin message
        assertEquals(Long.valueOf(editMessage.getChatId()).longValue(), CHAT_ID);
    }


    @ParameterizedTest
    @MethodSource("tracksRange")
    public void onUpdateReceived_pickTracksFromTrackMenuCommand_correctTrackSent(int trackIdx) throws IOException, URISyntaxException, TelegramApiException {
        //GIVEN
        String findQuery = "some query";
        Track correctTrack = someTrackList.get(trackIdx);
        when(loader.findTracks(findQuery, TRACK_MENU_PAGE_SIZE * TRACK_MENU_PAGE_COUNT)).thenReturn(someTrackList);
        when(trackMenuRepository.findById(CHAT_ID)).thenReturn(Optional.of(new TrackMenu(CHAT_ID, someTrackList)));
        when(trackMenuRepository.existsById(CHAT_ID)).thenReturn(true);
        when(loader.download(correctTrack.getUrl())).thenReturn(correctTrack);

        //WHEN
        bot.onUpdateReceived(getUpdateWithMessage("/" + trackIdx)); // pick track

        //THEN
        ArgumentCaptor<SendAudio> sendAudioArgument = ArgumentCaptor.forClass(SendAudio.class);
        verify(bot, times(1)).send(sendAudioArgument.capture()); // capture edited message
        SendAudio sendAudio = sendAudioArgument.getValue();
        assertEquals(sendAudio.getPerformer(), correctTrack.getPerformer());
        assertEquals(sendAudio.getTitle(), correctTrack.getTitle());
        assertEquals(sendAudio.getAudio().getNewMediaFile(), correctTrack.getFile());
        assertEquals(Long.valueOf(sendAudio.getChatId()).longValue(), CHAT_ID);
    }

    private static IntStream tracksRange() {
        return IntStream.range(0, TRACK_MENU_PAGE_COUNT * TRACK_MENU_PAGE_SIZE);
    }

    private static IntStream trackMenuPagesRange() {
        return IntStream.range(0, TRACK_MENU_PAGE_COUNT);
    }

    private static String getPageButtonCallbackData(int pageIdx) {
        return String.format("TM:%d", pageIdx);
    }

    private static Update getUpdateWithMessage(String msg) {
        Update mockedUpdate = mock(Update.class);
        Message mockedMessage = getMessage(msg);

        when(mockedUpdate.getMessage()).thenReturn(mockedMessage);
        when(mockedUpdate.hasMessage()).thenReturn(true);
        return mockedUpdate;
    }


    private static Update getUpdateWithCallbackQuery(String data) {
        Update mockedUpdate = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message mockedMessage = getMessage("");

        when(callbackQuery.getData()).thenReturn(data);
        when(callbackQuery.getMessage()).thenReturn(mockedMessage);

        when(mockedUpdate.getCallbackQuery()).thenReturn(callbackQuery);
        when(mockedUpdate.hasCallbackQuery()).thenReturn(true);
        return mockedUpdate;
    }

    private static Message getMessage(String msg) {
        Message mockedMessage = mock(Message.class);

        when(mockedMessage.hasText()).thenReturn(true);
        when(mockedMessage.getText()).thenReturn(msg);
        when(mockedMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockedMessage.getMessageId()).thenReturn(MESSAGE_ID);
        return mockedMessage;
    }

}