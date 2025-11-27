package svinstvo.b4b.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "b4b.processor")
@Data
public class SchedulerConfig {
    private Integer batchSize = 50;
    private String scheduleCron;
}