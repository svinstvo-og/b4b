package svinstvo.b4b.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import svinstvo.b4b.model.AppConfig;
import svinstvo.b4b.model.RawTransaction;
import svinstvo.b4b.repository.AppConfigRepository;
import svinstvo.b4b.repository.RawTransactionRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionService {

    private final RawTransactionRepository rawTransactionRepository;
    private final AppConfigRepository appConfigRepository;

    private static final String LAST_UPDATE_ID_KEY = "last_telegram_update_id";

    @Transactional
    public void saveRawMessage(Message message) {
        if (message.getText() == null || message.getText().isBlank()) {
            log.debug("Skipping empty message");
            return;
        }

        // Check for duplicates
        Optional<RawTransaction> existing = rawTransactionRepository
                .findByTelegramMessageId(message.getMessageId());

        if (existing.isPresent()) {
            log.debug("Message {} already exists, skipping", message.getMessageId());
            return;
        }

        // Convert Unix timestamp to LocalDateTime
        LocalDateTime receivedAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(message.getDate()),
                ZoneId.systemDefault()
        );

        RawTransaction rawTransaction = RawTransaction.builder()
                .telegramMessageId(message.getMessageId())
                .telegramChatId(message.getChatId())
                .messageText(message.getText())
                .receivedAt(receivedAt)
                .isProcessed(false)
                .build();

        rawTransactionRepository.save(rawTransaction);

        log.info("Saved raw transaction from message ID: {} - Text: '{}'",
                message.getMessageId(),
                message.getText().substring(0, Math.min(50, message.getText().length())));
    }

    public Integer getLastUpdateId() {
        return appConfigRepository.findById(LAST_UPDATE_ID_KEY)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(0);
    }

    @Transactional
    public void updateLastUpdateId(Integer updateId) {
        AppConfig config = appConfigRepository.findById(LAST_UPDATE_ID_KEY)
                .orElse(AppConfig.builder()
                        .configKey(LAST_UPDATE_ID_KEY)
                        .build());

        config.setConfigValue(String.valueOf(updateId));
        appConfigRepository.save(config);

        log.debug("Updated last_telegram_update_id to: {}", updateId);
    }

    public boolean isCommand(String text) {
        return text != null && text.startsWith("/");
    }
}