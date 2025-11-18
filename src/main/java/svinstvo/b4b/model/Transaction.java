package svinstvo.b4b.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_transaction_id")
    private Long rawTransactionId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency = "CZK";

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "sentiment_tag", length = 50)
    private String sentimentTag;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}