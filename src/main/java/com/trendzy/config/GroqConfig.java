package com.trendzy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "groq")
@Data
public class GroqConfig {
    private int batchSize;           // from application.yml: groq.batch-size
    private long rateLimitPauseMs;   // from application.yml: groq.rate-limit-pause-ms
    private int dailyTokenLimit;     // from application.yml: groq.daily-token-limit
}