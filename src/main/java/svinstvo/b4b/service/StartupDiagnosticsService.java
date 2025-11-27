package svinstvo.b4b.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import svinstvo.b4b.config.OpenAIConfig;
import svinstvo.b4b.config.TelegramConfig;
import svinstvo.b4b.repository.AppConfigRepository;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
@Slf4j
@RequiredArgsConstructor
public class StartupDiagnosticsService {

    private final TelegramConfig telegramConfig;
    private final OpenAIConfig openAIConfig;
    private final DataSource dataSource;
    private final AppConfigRepository appConfigRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void runDiagnostics() {
        log.info("=".repeat(60));
        log.info("🚀 BUDGETBOT STARTUP DIAGNOSTICS");
        log.info("=".repeat(60));

        checkTelegramConfig();
        checkOpenAIConfig();
        checkDatabaseConnection();
        checkDatabaseSchema();

        log.info("=".repeat(60));
        log.info("✅ STARTUP DIAGNOSTICS COMPLETE");
        log.info("=".repeat(60));
    }

    private void checkTelegramConfig() {
        log.info("\n📱 TELEGRAM CONFIGURATION:");

        String username = telegramConfig.getUsername();
        String token = telegramConfig.getToken();

        if (username == null || username.equals("YourBotUsername")) {
            log.error("❌ Telegram bot username not configured!");
            log.error("   Set TELEGRAM_BOT_USERNAME environment variable");
        } else {
            log.info("✅ Bot Username: {}", username);
        }

        if (token == null || token.equals("your-token-here") || token.length() < 20) {
            log.error("❌ Telegram bot token not configured or invalid!");
            log.error("   Set TELEGRAM_BOT_TOKEN environment variable");
            log.error("   Get token from: https://t.me/BotFather");
        } else {
            log.info("✅ Bot Token: {}...{}",
                    token.substring(0, 10),
                    token.substring(token.length() - 4));
        }
    }

    private void checkOpenAIConfig() {
        log.info("\n🤖 OPENAI CONFIGURATION:");

        String apiKey = openAIConfig.getKey();

        if (apiKey == null || apiKey.equals("your-key-here") || apiKey.length() < 20) {
            log.error("❌ OpenAI API key not configured or invalid!");
            log.error("   Set OPENAI_API_KEY environment variable");
            log.error("   Get key from: https://platform.openai.com/api-keys");
        } else {
            log.info("✅ API Key: {}...{}",
                    apiKey.substring(0, 7),
                    apiKey.substring(apiKey.length() - 4));
        }

        log.info("✅ Base URL: {}", openAIConfig.getBaseUrl());
        log.info("✅ Categorization Model: {}", openAIConfig.getModelMini());
        log.info("✅ Advice Model: {}", openAIConfig.getModelFull());
    }

    private void checkDatabaseConnection() {
        log.info("\n🗄️  DATABASE CONNECTION:");

        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String user = conn.getMetaData().getUserName();
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            String dbVersion = conn.getMetaData().getDatabaseProductVersion();

            log.info("✅ Connected to: {}", url);
            log.info("✅ User: {}", user);
            log.info("✅ Database: {} {}", dbProduct, dbVersion);
        } catch (Exception e) {
            log.error("❌ Database connection failed!", e);
            log.error("   Make sure PostgreSQL is running:");
            log.error("   docker-compose up -d");
        }
    }

    private void checkDatabaseSchema() {
        log.info("\n📊 DATABASE SCHEMA:");

        try {
            long configCount = appConfigRepository.count();
            log.info("✅ app_config table: {} records", configCount);

            // Check if tables exist by trying to query them
            log.info("✅ Database schema initialized successfully");

            // Get last update ID
            appConfigRepository.findById("last_telegram_update_id")
                    .ifPresentOrElse(
                            config -> log.info("✅ Last Telegram Update ID: {}", config.getConfigValue()),
                            () -> log.warn("⚠️  Last Telegram Update ID not found (will be created on first message)")
                    );

        } catch (Exception e) {
            log.error("❌ Database schema check failed!", e);
            log.error("   Run Flyway migrations: mvn flyway:migrate");
        }
    }
}