package com.github.andluu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.telegram.telegrambots.ApiContextInitializer;

@Configuration
@PropertySources({
        @PropertySource("classpath:config.properties"),
        @PropertySource("classpath:strings.properties")
})
@ComponentScan(basePackages = "com.github.andluu")
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        try {
            ApiContextInitializer.init();
            AnnotationConfigApplicationContext ctx =
                    new AnnotationConfigApplicationContext(Application.class);
        } catch (Exception e) {
            LOG.error("Context loading error", e);
        }
    }
}