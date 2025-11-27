package svinstvo.b4b.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import svinstvo.b4b.config.TelegramConfig;

@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramConfig telegramConfig;
    private final IngestionService ingestionService;
    private final BatchProcessorService batchProcessorService;
    private final ReportingService reportingService;

    public TelegramBotService(
            TelegramConfig telegramConfig,
            IngestionService ingestionService,
            BatchProcessorService batchProcessorService,
            ReportingService reportingService) {
        super(telegramConfig.getToken());
        this.telegramConfig = telegramConfig;
        this.ingestionService = ingestionService;
        this.batchProcessorService = batchProcessorService;
        this.reportingService = reportingService;

        log.info("TelegramBotService initialized with bot: {}", telegramConfig.getUsername());
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        try {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            log.info("Received message from chat {}: {}", chatId, messageText);

            // Handle commands
            if (ingestionService.isCommand(messageText)) {
                handleCommand(chatId, messageText);
            } else {
                // Save as expense entry
                ingestionService.saveRawMessage(update.getMessage());
                sendMessage(chatId, "✅ Expense logged! Use /sync to process immediately.");
            }

            // Update last processed update ID
            ingestionService.updateLastUpdateId(update.getUpdateId());

        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleCommand(Long chatId, String command) {
        log.info("Processing command: {}", command);

        try {
            switch (command.toLowerCase().split(" ")[0]) {
                case "/start" -> handleStart(chatId);
                case "/sync" -> handleSync(chatId);
                case "/stats" -> handleStats(chatId);
                case "/advice" -> handleAdvice(chatId);
                case "/status" -> handleStatus(chatId);
                case "/help" -> handleHelp(chatId);
                default -> sendMessage(chatId, "Unknown command. Type /help for available commands.");
            }
        } catch (Exception e) {
            log.error("Error handling command: {}", command, e);
            sendMessage(chatId, "❌ Error processing command. Please try again.");
        }
    }

    private void handleStart(Long chatId) {
        String welcomeMessage = """
                👋 Welcome to BudgetBot!
                
                I'm your personal expense tracker. Just send me messages like:
                • "Beer and chips 250"
                • "Rent 15000 czk"
                • "Groceries 850"
                
                I'll automatically categorize and track your spending!
                
                Commands:
                /stats - Quick spending summary
                /advice - Get AI financial advice
                /sync - Process pending transactions
                /status - System status
                /help - Show this help
                """;

        sendMessage(chatId, welcomeMessage);
    }

    private void handleSync(Long chatId) {
        sendMessage(chatId, "⏳ Processing pending transactions...");

        try {
            batchProcessorService.processPendingTransactions();
            sendMessage(chatId, "✅ All pending transactions processed!");
        } catch (Exception e) {
            log.error("Error during sync", e);
            sendMessage(chatId, "❌ Error processing transactions. Check logs.");
        }
    }

    private void handleStats(Long chatId) {
        try {
            String stats = reportingService.generateQuickStats();
            sendMessage(chatId, stats);
        } catch (Exception e) {
            log.error("Error generating stats", e);
            sendMessage(chatId, "❌ Error generating statistics.");
        }
    }

    private void handleAdvice(Long chatId) {
        sendMessage(chatId, "🤔 Analyzing your spending... This may take a moment.");

        try {
            String advice = reportingService.generateFinancialAdvice(5000.0);
            sendMessage(chatId, advice);
        } catch (Exception e) {
            log.error("Error generating advice", e);
            sendMessage(chatId, "❌ Error generating advice. Please try again later.");
        }
    }

    private void handleStatus(Long chatId) {
        try {
            String status = reportingService.getSystemStatus();
            sendMessage(chatId, status);
        } catch (Exception e) {
            log.error("Error getting status", e);
            sendMessage(chatId, "❌ Error retrieving system status.");
        }
    }

    private void handleHelp(Long chatId) {
        String helpMessage = """
                🤖 BudgetBot Commands
                
                /start - Welcome message
                /stats - Monthly spending summary
                /advice - AI financial advisor
                /sync - Manually process pending transactions
                /status - System status and stats
                /help - Show this help
                
                📝 Just send any text to log an expense!
                Example: "Coffee 85 czk"
                """;

        sendMessage(chatId, helpMessage);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chat {}", chatId, e);
        }
    }
}