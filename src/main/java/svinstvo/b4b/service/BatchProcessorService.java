package svinstvo.b4b.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import svinstvo.b4b.config.SchedulerConfig;
import svinstvo.b4b.dto.ParsedTransaction;
import svinstvo.b4b.model.RawTransaction;
import svinstvo.b4b.model.Transaction;
import svinstvo.b4b.repository.RawTransactionRepository;
import svinstvo.b4b.repository.TransactionRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchProcessorService {

    private final RawTransactionRepository rawTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final OpenAIService openAIService;
    private final SchedulerConfig schedulerConfig;

    @Scheduled(cron = "${budgetbot.processor.schedule-cron}")
    public void scheduledProcessing() {
        log.debug("Scheduled batch processing triggered");
        processPendingTransactions();
    }

    @Transactional
    public void processPendingTransactions() {
        Long pendingCount = rawTransactionRepository.countByIsProcessed(false);

        if (pendingCount == 0) {
            log.debug("No pending transactions to process");
            return;
        }

        log.info("Found {} pending transactions. Starting batch processing...", pendingCount);

        // Fetch batch
        List<RawTransaction> pendingTransactions = fetchPendingBatch();

        if (pendingTransactions.isEmpty()) {
            return;
        }

        try {
            // Parse with OpenAI
            List<ParsedTransaction> parsedTransactions = openAIService.parseTransactions(pendingTransactions);

            // Create lookup map for matching
            Map<Long, RawTransaction> rawTransactionMap = pendingTransactions.stream()
                    .collect(Collectors.toMap(RawTransaction::getId, rt -> rt));

            // Save parsed transactions
            for (ParsedTransaction parsed : parsedTransactions) {
                try {
                    RawTransaction rawTx = rawTransactionMap.get(parsed.getId());

                    if (rawTx == null) {
                        log.warn("Could not find raw transaction with ID: {}", parsed.getId());
                        continue;
                    }

                    // Create and save normalized transaction
                    Transaction transaction = Transaction.builder()
                            .rawTransactionId(rawTx.getId())
                            .itemName(parsed.getItemName())
                            .amount(parsed.getAmount())
                            .currency(parsed.getCurrency() != null ? parsed.getCurrency() : "CZK")
                            .category(parsed.getCategory())
                            .sentimentTag(parsed.getSentimentTag())
                            .build();

                    transactionRepository.save(transaction);

                    // Mark as processed
                    rawTx.setIsProcessed(true);
                    rawTransactionRepository.save(rawTx);

                    log.debug("Successfully processed transaction ID: {} - {}", rawTx.getId(), parsed.getItemName());

                } catch (Exception e) {
                    log.error("Error saving transaction ID: {}", parsed.getId(), e);
                    RawTransaction rawTx = rawTransactionMap.get(parsed.getId());
                    if (rawTx != null) {
                        rawTx.setErrorLog("Error during save: " + e.getMessage());
                        rawTransactionRepository.save(rawTx);
                    }
                }
            }

            log.info("Successfully processed {} transactions", parsedTransactions.size());

        } catch (Exception e) {
            log.error("Error during batch processing", e);

            // Mark all as error
            for (RawTransaction rawTx : pendingTransactions) {
                rawTx.setErrorLog("Batch processing failed: " + e.getMessage());
                rawTransactionRepository.save(rawTx);
            }
        }
    }

    private List<RawTransaction> fetchPendingBatch() {
        int batchSize = schedulerConfig.getBatchSize();

        // Note: Native query approach for LIMIT support
        return rawTransactionRepository.findUnprocessedTransactions()
                .stream()
                .limit(batchSize)
                .toList();
    }

    public void processSingleTransaction(Long rawTransactionId) {
        RawTransaction rawTx = rawTransactionRepository.findById(rawTransactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + rawTransactionId));

        if (rawTx.getIsProcessed()) {
            log.info("Transaction {} already processed", rawTransactionId);
            return;
        }

        try {
            List<ParsedTransaction> parsed = openAIService.parseTransactions(List.of(rawTx));

            if (!parsed.isEmpty()) {
                ParsedTransaction parsedTx = parsed.get(0);

                Transaction transaction = Transaction.builder()
                        .rawTransactionId(rawTx.getId())
                        .itemName(parsedTx.getItemName())
                        .amount(parsedTx.getAmount())
                        .currency(parsedTx.getCurrency() != null ? parsedTx.getCurrency() : "CZK")
                        .category(parsedTx.getCategory())
                        .sentimentTag(parsedTx.getSentimentTag())
                        .build();

                transactionRepository.save(transaction);
                rawTx.setIsProcessed(true);
                rawTransactionRepository.save(rawTx);

                log.info("Successfully processed single transaction: {}", rawTransactionId);
            }

        } catch (Exception e) {
            log.error("Error processing single transaction: {}", rawTransactionId, e);
            rawTx.setErrorLog(e.getMessage());
            rawTransactionRepository.save(rawTx);
        }
    }
}