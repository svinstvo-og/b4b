package svinstvo.b4b.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import svinstvo.b4b.repository.RawTransactionRepository;
import svinstvo.b4b.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportingService {

    private final TransactionRepository transactionRepository;
    private final RawTransactionRepository rawTransactionRepository;
    private final OpenAIService openAIService;

    public String generateQuickStats() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        BigDecimal totalSpent = transactionRepository.sumAmountSince(startOfMonth);
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        Long transactionCount = transactionRepository.countTransactionsSince(startOfMonth);
        Long pendingCount = rawTransactionRepository.countByIsProcessed(false);

        return String.format("""
                📊 Monthly Stats (Since %s)
                
                💰 Total Spent: %.2f CZK
                📝 Transactions: %d
                ⏳ Pending Processing: %d
                """,
                startOfMonth.toLocalDate(),
                totalSpent.doubleValue(),
                transactionCount,
                pendingCount
        );
    }

    public String generateDetailedReport() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        BigDecimal totalSpent = transactionRepository.sumAmountSince(startOfMonth);
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        List<Object[]> topCategories = transactionRepository.findTopCategoriesSince(startOfMonth);

        StringBuilder report = new StringBuilder();
        report.append("📊 Monthly Financial Report\n\n");
        report.append(String.format("💰 Total Spent: %.2f CZK\n\n", totalSpent.doubleValue()));

        if (!topCategories.isEmpty()) {
            report.append("📈 Top Spending Categories:\n");
            int rank = 1;
            for (Object[] category : topCategories) {
                String categoryName = (String) category[0];
                BigDecimal amount = (BigDecimal) category[1];
                double percentage = (amount.doubleValue() / totalSpent.doubleValue()) * 100;

                report.append(String.format("%d. %s: %.2f CZK (%.1f%%)\n",
                        rank++, categoryName, amount.doubleValue(), percentage));

                if (rank > 5) break; // Top 5 only
            }
        } else {
            report.append("No transactions found for this month.\n");
        }

        return report.toString();
    }

    public String generateFinancialAdvice(Double savingsGoal) {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        String spendingSummary = generateDetailedReport();

        if (savingsGoal == null) {
            savingsGoal = 5000.0; // Default goal
        }

        log.info("Generating financial advice with goal: {} CZK", savingsGoal);

        String advice = openAIService.generateFinancialAdvice(spendingSummary, savingsGoal);

        return String.format("""
                🎯 Savings Goal: %.2f CZK
                
                %s
                
                %s
                """,
                savingsGoal,
                spendingSummary,
                advice
        );
    }

    public String getSystemStatus() {
        Long totalTransactions = transactionRepository.count();
        Long totalRaw = rawTransactionRepository.count();
        Long pendingCount = rawTransactionRepository.countByIsProcessed(false);
        Long processedCount = rawTransactionRepository.countByIsProcessed(true);

        return String.format("""
                🤖 BudgetBot System Status
                
                📊 Database Stats:
                • Total Transactions: %d
                • Total Messages Received: %d
                • Processed: %d
                • Pending: %d
                
                ✅ System: Operational
                """,
                totalTransactions,
                totalRaw,
                processedCount,
                pendingCount
        );
    }
}