package svinstvo.b4b.config;

import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Data
@Slf4j
public class OpenAIConfig {
    private String key;
    private String baseUrl;
    private String modelMini;
    private String modelFull;

    @Bean
    public WebClient openAIWebClient() {
        log.info("!!!!!!!!!!!!!!!!!!!! OpenAIKey - {}, baseUrl = {}", key, baseUrl);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + key)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}