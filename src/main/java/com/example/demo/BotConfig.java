package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        try {
            // Directly create an instance of TelegramBotsApi without calling ApiContextInitializer.init();
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new MyBot());
            return api;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
