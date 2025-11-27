-- Raw transactions table
CREATE TABLE raw_transactions (
                                  id BIGSERIAL PRIMARY KEY,
                                  telegram_message_id INTEGER NOT NULL UNIQUE,
                                  telegram_chat_id BIGINT NOT NULL,
                                  message_text TEXT NOT NULL,
                                  received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  is_processed BOOLEAN DEFAULT FALSE,
                                  error_log TEXT
);

CREATE INDEX idx_raw_transactions_processed ON raw_transactions(is_processed);
CREATE INDEX idx_raw_transactions_received_at ON raw_transactions(received_at);

-- Normalized transactions table
CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              raw_transaction_id BIGINT REFERENCES raw_transactions(id),
                              item_name VARCHAR(255),
                              amount DECIMAL(10, 2),
                              currency VARCHAR(3) DEFAULT 'CZK',
                              category VARCHAR(50),
                              transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              sentiment_tag VARCHAR(50)
);

CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_category ON transactions(category);

-- App configuration table
CREATE TABLE app_config (
                            config_key VARCHAR(50) PRIMARY KEY,
                            config_value TEXT
);

-- Seed initial config
INSERT INTO app_config (config_key, config_value)
VALUES ('last_telegram_update_id', '0');