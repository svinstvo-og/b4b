package svinstvo.b4b.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import svinstvo.b4b.service.TelegramBotService;

@Configuration
@Slf4j
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        try {
            log.info("Initializing Telegram Bot API...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            log.info("✅ Telegram bot registered successfully: {}", telegramBotService.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot", e);
            throw new RuntimeException("Could not register Telegram bot", e);
        }
    }
}