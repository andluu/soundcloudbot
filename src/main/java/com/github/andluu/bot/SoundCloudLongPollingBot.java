package com.github.andluu.bot;

import com.github.andluu.config.BotConfig;
import com.github.andluu.loader.SoundCloudTrackLoader;
import com.github.andluu.model.Track;
import com.github.andluu.model.TrackMenu;
import com.github.andluu.repositories.TrackMenuRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class SoundCloudLongPollingBot extends TelegramLongPollingBot {

    private static final Logger LOG = LoggerFactory.getLogger(SoundCloudLongPollingBot.class);
    /**
     * Count of pages in TrackMenu
     */
    static final int TRACK_MENU_PAGE_COUNT = 5;
    /**
     * Count of tracks by each page in TrackMenu
     */
    static final int TRACK_MENU_PAGE_SIZE = 10;

    private SoundCloudTrackLoader trackLoader;
    private TrackMenuRepository trackMenuRepository;
    private final BotConfig botConfig;

    @PostConstruct
    public void init() throws TelegramApiRequestException {
        LOG.debug("Creating new SoundCloudLongPollingBot");
        TelegramBotsApi api = new TelegramBotsApi();
        api.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            LOG.debug("Incoming update {}", update.toString().replaceAll("\n", "\\n"));

            if (update.hasMessage() && update.getMessage().hasText()) {
                handleIncomingMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleIncomingCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            LOG.error("SEVERE: BOTSESSION", e);
        }
    }

    private void handleIncomingCallbackQuery(CallbackQuery cq) {
        String cqData = cq.getData();
        long chatId = cq.getMessage().getChatId();
        int messageId = cq.getMessage().getMessageId();

        LOG.debug("Received callback query data='{}', messageId={}, chatId={}", cqData, messageId, chatId);

        if (isCallbackFromTrackMenu(cqData)) {
            int selectedPageIdx = Integer.parseInt(cqData.split(":")[1]);
            processButtonTrackMenuPage(selectedPageIdx, chatId, messageId);
        }
    }

    private boolean isCallbackFromTrackMenu(final String cqData) {
        return cqData.split(":")[0].equals(botConfig.getButtonCallDataTmpPageIdxText().split(":")[0]);
    }

    private void handleIncomingMessage(Message msg) {
        String text = msg.getText();
        long chatId = msg.getChatId();

        LOG.debug("Received message id={}, text='{}', chatId={}", msg.getMessageId(), text.replaceAll("\n", " "), chatId);

        if (text.startsWith("/")) {
            String cmd = text.substring(1);
            if (isNumber(cmd)) {
                trackIdxCommand(Integer.parseInt(cmd), chatId);
            } else if (cmd.equals("start")) {
                helpCommand(chatId);
            } else if (cmd.equals("help")) {
                helpCommand(chatId);
            }
        } else if (containsLink(text)) {
            downloadTrackByLink(parseLink(text), chatId);
        } else {
            findQuery(text, chatId);
        }
    }

    private void helpCommand(long chatId) {
        LOG.info("Processing start/help command from chatId={}", chatId);

        sendToChat(chatId, botConfig.getMessageHelpText());
    }

    private void trackIdxCommand(int trackIdx, long chatId) {
        LOG.info("Processing track index={}, from chatId={}", trackIdx, chatId);

        Optional<TrackMenu> trackMenuOptional = trackMenuRepository.findById(chatId);
        if (!trackMenuOptional.isPresent()) {
            sendToChat(chatId, botConfig.getMessageMenuDoesNotExist());
            return;
        }

        String trackUrl = trackMenuOptional.get().getFoundTracks().get(trackIdx).getUrl();
        downloadAndSendTrack(trackUrl, chatId);
    }

    private void downloadTrackByLink(String link, long chatId) {
        LOG.info("Processing link={}, from chatId={}", link, chatId);

        downloadAndSendTrack(link, chatId);
    }

    private void downloadAndSendTrack(final String link, final long chatId) {
        Track downloadedTrack = null;
        try {
            sendChatActionAudioSending(chatId);
            downloadedTrack = trackLoader.download(link);
            sendTrackToChat(chatId, downloadedTrack);
        } catch (IOException e) {
            LOG.error("Failed to download from link={}", link, e);
            sendToChat(chatId, botConfig.getMessageDownloadError());
        } finally {
            if (downloadedTrack != null)
                downloadedTrack.getFile().delete();
        }
    }

    private void findQuery(String findQuery, long chatId) {
        LOG.info("Processing findQuery='{}', from chatId={}", findQuery.replaceAll("\n", " "), chatId);

        try {
            List<Track> foundTracks = trackLoader.findTracks(findQuery, TRACK_MENU_PAGE_SIZE * TRACK_MENU_PAGE_COUNT);

            if (foundTracks.isEmpty()) {
                LOG.info("Nothing found");
                sendToChat(chatId, String.format(botConfig.getMessageNoTracksFound(), findQuery));
                return;
            }

            LOG.info("Found {} tracks", foundTracks.size());

            TrackMenu trackMenu = new TrackMenu(chatId, foundTracks);
            trackMenuRepository.save(trackMenu);

            sendToChat(chatId,
                    getTrackListString(0, trackMenu),
                    createTrackMenuPagesMarkup(0, trackMenu.getFoundTracks().size()));
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to find tracks by findQuery='{}', for chatId={}", findQuery.replaceAll("\n", " "), chatId, e);
            sendToChat(chatId, String.format(botConfig.getMessageFindError(), findQuery));
        }
    }

    private void processButtonTrackMenuPage(int pageIdx, long chatId, int messageId) {
        LOG.info("Processing TM page button idx={}, from chatId={}, from messageId={}", pageIdx, chatId, messageId);

        Optional<TrackMenu> trackMenuOptional = trackMenuRepository.findById(chatId);
        if (!trackMenuOptional.isPresent()) {
            sendEditedToChat(chatId, messageId, botConfig.getMessageMenuDoesNotExist());
            return;
        }

        TrackMenu trackMenu = trackMenuOptional.get();
        LOG.debug("Fetched {} tracks from trackMenu by chatId={}", trackMenu.getFoundTracks().size(), trackMenu.getChatId());

        sendEditedToChat(chatId,
                messageId,
                getTrackListString(pageIdx, trackMenu),
                createTrackMenuPagesMarkup(pageIdx, trackMenu.getFoundTracks().size()));
    }

    private String getTrackListString(int pageIdx, TrackMenu trackMenu) {
        return trackMenu.getFoundTracks().stream()
                .skip(pageIdx * TRACK_MENU_PAGE_SIZE)
                .limit(TRACK_MENU_PAGE_SIZE)
                .map(track -> String.format(botConfig.getMessageTrack(),
                        trackMenu.getFoundTracks().indexOf(track), track.getPerformer(), track.getTitle()))
                .collect(Collectors.joining());
    }

    private void sendChatActionAudioSending(long chatId) {
        try {
            send(new SendChatAction(chatId, "upload_audio"));
        } catch (TelegramApiException e) {
            LOG.error("Failed send chat action 'upload_audio' to chatId={}", chatId, e);
        }
    }

    private void sendToChat(long chatId, String text) {
        LOG.debug("Sending to chatId={}, text='{}'", chatId, text.replaceAll("\n", " "));
        try {
            send(new SendMessage(chatId, text));
        } catch (TelegramApiException e) {
            LOG.error("Failed send message text='{}'; to chatId={}", text.replaceAll("\n", " "), chatId, e);
        }
    }

    private void sendToChat(long chatId, String text, InlineKeyboardMarkup markup) {
        LOG.debug("Sending with markup to chatId={}, text='{}'", chatId, text.replaceAll("\n", " "));
        try {
            send(new SendMessage(chatId, text).setReplyMarkup(markup));
        } catch (TelegramApiException e) {
            LOG.error("Failed send text='{}', with markup={}, to chatId={}", text.replaceAll("\n", " "), markup, chatId, e);
        }
    }

    private void sendEditedToChat(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        LOG.debug("Sending edit for msgId={} with markup to chatId={}, text='{}'", messageId, chatId, text.replaceAll("\n", " "));
        try {
            send(new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setText(text)
                    .setReplyMarkup(markup));
        } catch (TelegramApiException e) {
            LOG.error("Failed send edit for msgId={} with markup={}, to chatId={}, text='{}'", messageId, markup, chatId, text.replaceAll("\n", " "), e);
        }
    }

    private void sendEditedToChat(long chatId, int messageId, String text) {
        LOG.debug("Sending edit for msgId={} to chatId={}, text='{}'", messageId, chatId, text.replaceAll("\n", " "));
        try {
            send(new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setText(text));
        } catch (TelegramApiException e) {
            LOG.error("Failed send edit for msgId={} to chatId={}, text='{}'", messageId, chatId, text.replaceAll("\n", " "), e);
        }
    }

    private void sendTrackToChat(long chatId, Track downloadedTrack) {
        LOG.debug("Sending audio to chatId={}", chatId);
        try {
            send(new SendAudio()
                    .setTitle(downloadedTrack.getTitle())
                    .setPerformer(downloadedTrack.getPerformer())
                    .setAudio(downloadedTrack.getFile())
                    .setChatId(chatId));
        } catch (TelegramApiException e) {
            LOG.error("Failed to send track={}, to chatId={}", downloadedTrack, chatId, e);
            sendToChat(chatId, botConfig.getMessageFailedDueAudioSize());
        }
    }

    private InlineKeyboardMarkup createTrackMenuPagesMarkup(int currPageIdx, int tracksCount) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        int pagesCount = (int) Math.ceil((double) tracksCount / TRACK_MENU_PAGE_SIZE);
        List<InlineKeyboardButton> buttons = IntStream.range(0, pagesCount)
                .mapToObj(pageIdx -> new InlineKeyboardButton(pageIdx == currPageIdx ?
                        String.format(botConfig.getButtonTextCurrPageIdx(), pageIdx) :
                        String.valueOf(pageIdx))
                        .setCallbackData(String.format(botConfig.getButtonCallDataTmpPageIdxText(), pageIdx)))
                .collect(Collectors.toList());

        keyboard.add(buttons);
        return markup.setKeyboard(keyboard);
    }

    /**
     * Generic version of {@link org.telegram.telegrambots.meta.bots.AbsSender#execute} methods.
     * Redirects calls to {@link org.telegram.telegrambots.meta.bots.AbsSender#execute} family.
     * <p>
     * Supports {@link BotApiMethod}, {@link SendAudio}
     *
     * @param method parameter to be passed to {@link org.telegram.telegrambots.meta.bots.AbsSender#execute}.
     *               By now supports only {@link BotApiMethod}, {@link SendAudio}
     * @throws TelegramApiException
     */
    @SuppressWarnings("unchecked")
    // Use this method instead of execute!! Needed for testing purposes.
    void send(PartialBotApiMethod<? extends Serializable> method) throws TelegramApiException {
        LOG.debug("Redirecting to execute instance of class={}", method.getClass());

        if (method instanceof SendChatAction)
            execute((SendChatAction) method);
        else if (method instanceof BotApiMethod)
            execute((BotApiMethod) method);
        else if (method instanceof SendAudio)
            execute((SendAudio) method);
    }

    // Needed for testing purposes.
    void setTrackLoader(SoundCloudTrackLoader trackLoader) {
        LOG.trace("Changed trackLoader");
        this.trackLoader = trackLoader;
    }

    // Needed for testing purposes.
    void setTrackMenuRepository(TrackMenuRepository trackMenuRepository) {
        LOG.trace("Changed trackMenuRepository");
        this.trackMenuRepository = trackMenuRepository;
    }

    private static boolean containsLink(String text) {
        LOG.trace("Checking whether text contains a link '{}'", text.replaceAll("\n", " "));
        return BotConfig.LINK_PATTERN.matcher(text).find();
    }

    private static boolean isNumber(String command) {
        return command.matches("\\d+");
    }

    // Retrieve a url from a given String
    private static String parseLink(String text) {
        LOG.trace("Finding url in a text={}", text.replaceAll("\n", " "));

        Matcher m = BotConfig.LINK_PATTERN.matcher(text);
        if (m.find()) {
            return text.substring(m.start(), m.end());
        } else {
            throw new IllegalArgumentException("Text \"" + text.replaceAll("\n", " ") + "\" does not contain url");
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    @PreDestroy
    public void onClosing() {
        super.onClosing();
    }
}
