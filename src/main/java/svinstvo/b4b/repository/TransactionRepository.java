package svinstvo.b4b.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import svinstvo.b4b.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionDate >= :startDate")
    BigDecimal sumAmountSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT t.category, SUM(t.amount) as total FROM Transaction t " +
            "WHERE t.transactionDate >= :startDate " +
            "GROUP BY t.category ORDER BY total DESC")
    List<Object[]> findTopCategoriesSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :startDate")
    Long countTransactionsSince(@Param("startDate") LocalDateTime startDate);
}