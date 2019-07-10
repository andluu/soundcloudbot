package com.github.schednie.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BotConfig {

    public static String BOT_TOKEN;
    public static String BOT_USERNAME;
    private static Properties STRINGS;

    private static final Logger LOG = LoggerFactory.getLogger(BotConfig.class);

    static {
        Properties propConfig = new Properties();
        STRINGS = new Properties();
        String configProperties = "config.properties";
        String stringProperties = "strings.properties";

        try (InputStream confIs = BotConfig.class.getClassLoader().getResourceAsStream(configProperties);
             InputStream stringsIs = BotConfig.class.getClassLoader().getResourceAsStream(stringProperties)) {

            propConfig.load(confIs);
            BOT_USERNAME = propConfig.getProperty("BOT_USERNAME");
            BOT_TOKEN = propConfig.getProperty("BOT_TOKEN");

            STRINGS.load(stringsIs);
        } catch (IOException e) {
            LOG.error("Failed to load some properties", e);
        }
    }

    // use this method to access strings.properties
    public static String getString(String propertyName) {
        return STRINGS.getProperty(propertyName, "<error>");
    }


}
