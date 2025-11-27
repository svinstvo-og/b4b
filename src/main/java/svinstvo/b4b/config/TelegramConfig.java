package svinstvo.b4b.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLOutput;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class TelegramConfig {
    private String username = System.getenv("TELEGRAM_USERNAME");
    private String token = System.getenv("TELEGRAM_TOKEN");
}