package svinstvo.b4b.repository;

import svinstvo.b4b.model.RawTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RawTransactionRepository extends JpaRepository<RawTransaction, Long> {

    Optional<RawTransaction> findByTelegramMessageId(Integer telegramMessageId);

    @Query("SELECT r FROM RawTransaction r WHERE r.isProcessed = false ORDER BY r.receivedAt ASC")
    List<RawTransaction> findUnprocessedTransactions();

    @Query(value = "SELECT r FROM RawTransaction r WHERE r.isProcessed = false ORDER BY r.receivedAt ASC LIMIT :limit")
    List<RawTransaction> findUnprocessedTransactionsWithLimit(int limit);

    Long countByIsProcessed(Boolean isProcessed);
}