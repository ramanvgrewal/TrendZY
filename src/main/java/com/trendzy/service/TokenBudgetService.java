package com.trendzy.service;

import com.trendzy.config.GroqConfig;
import com.trendzy.model.mongo.TokenUsage;
import com.trendzy.repository.mongo.TokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBudgetService {

    private final TokenUsageRepository tokenUsageRepository;
    private final GroqConfig groqConfig;

    public TokenUsage getTodayUsage() {
        LocalDate today = LocalDate.now();
        return tokenUsageRepository.findByDate(today).orElseGet(() -> {
            TokenUsage newUsage = TokenUsage.builder()
                    .date(today)
                    .tokensUsed(0)
                    .tokensRemaining(groqConfig.getDailyTokenLimit())
                    .batchesProcessed(0)
                    .batchesFailed(0)
                    .resetDate(LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0))
                    .build();
            return tokenUsageRepository.save(newUsage);
        });
    }

    public boolean hasEnoughBudget(int estimatedTokensNeeded) {
        TokenUsage usage = getTodayUsage();
        return usage.getTokensRemaining() >= estimatedTokensNeeded;
    }

    public void deductTokens(int tokensUsed, boolean isPipelineBatch, boolean success) {
        TokenUsage usage = getTodayUsage();
        usage.setTokensUsed(usage.getTokensUsed() + tokensUsed);
        usage.setTokensRemaining(Math.max(0, usage.getTokensRemaining() - tokensUsed));
        
        if (isPipelineBatch) {
            if (success) {
                usage.setBatchesProcessed(usage.getBatchesProcessed() + 1);
            } else {
                usage.setBatchesFailed(usage.getBatchesFailed() + 1);
            }
        }
        
        tokenUsageRepository.save(usage);
        log.info("Token budget updated: used={}, remaining={}", usage.getTokensUsed(), usage.getTokensRemaining());
    }

    public TokenUsage forceResetBudget() {
        TokenUsage usage = getTodayUsage();
        usage.setTokensUsed(0);
        usage.setTokensRemaining(groqConfig.getDailyTokenLimit());
        usage.setBatchesProcessed(0);
        usage.setBatchesFailed(0);
        return tokenUsageRepository.save(usage);
    }
}
