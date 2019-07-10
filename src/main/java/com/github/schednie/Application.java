package com.github.schednie;

import com.github.schednie.bot.SoundCloudLongPollingBot;
import com.github.schednie.config.BotConfig;
import com.github.schednie.config.DataConfig;
import com.github.schednie.loader.SoundCloudTrackLoader;
import com.github.schednie.loader.SoundCloudTrackLoaderImpl;
import com.github.schednie.repositories.TrackMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Configuration
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        try {
            AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class, DataConfig.class);
        } catch (Exception e) {
            LOG.error("Context loading error", e);
        }
    }

    @Bean(destroyMethod = "onClosing")
    TelegramLongPollingBot soundCloudBot(TrackMenuRepository repository) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        SoundCloudLongPollingBot bot =
                new SoundCloudLongPollingBot(soundCloudTrackLoader(), repository, BotConfig.BOT_TOKEN, BotConfig.BOT_USERNAME);
        api.registerBot(bot);
        return bot;
    }

    @Bean
    SoundCloudTrackLoader soundCloudTrackLoader() {
        return new SoundCloudTrackLoaderImpl();
    }
}




