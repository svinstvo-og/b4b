package svinstvo.b4b.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import svinstvo.b4b.config.TelegramConfig;
import svinstvo.b4b.repository.RawTransactionRepository;
import svinstvo.b4b.repository.TransactionRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class BotHealthController {

    private final TelegramConfig telegramConfig;
    private final RawTransactionRepository rawTransactionRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("bot_username", telegramConfig.getUsername());
        health.put("raw_transactions", rawTransactionRepository.count());
        health.put("processed_transactions", transactionRepository.count());
        health.put("pending_transactions", rawTransactionRepository.countByIsProcessed(false));

        return ResponseEntity.ok(health);
    }
}