package svinstvo.b4b.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_message_id", nullable = false, unique = true)
    private Integer telegramMessageId;

    @Column(name = "telegram_chat_id", nullable = false)
    private Long telegramChatId;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "is_processed")
    private Boolean isProcessed = false;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
