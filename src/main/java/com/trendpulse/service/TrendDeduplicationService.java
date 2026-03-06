package com.trendpulse.service;

import com.trendpulse.dto.AiAnalysisResult;
import com.trendpulse.entity.Trend;
import com.trendpulse.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * R4: Trend Deduplication and Merging Service.
 * Prevents duplicate trends by normalizing names and using Levenshtein
 * distance.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrendDeduplicationService {

    private final TrendRepository trendRepository;

    /**
     * Find an existing matching trend or create a new one from the AI result.
     */
    public Trend findOrCreateTrend(AiAnalysisResult result) {
        String newName = result.getProductName();
        String normalizedNew = normalize(newName);

        // Check all existing trends for a match
        List<Trend> allTrends = trendRepository.findAll();
        Optional<Trend> match = allTrends.stream()
                .filter(existing -> isSimilar(normalize(existing.getProductName()), normalizedNew))
                .findFirst();

        if (match.isPresent()) {
            // Merge into existing trend
            Trend existing = match.get();
            log.info("  🔀 Merged \"{}\" into existing trend \"{}\"", newName, existing.getProductName());

            existing.setMentionCountThisWeek(existing.getMentionCountThisWeek() + 1);
            existing.setTrendScore(Math.max(existing.getTrendScore(),
                    result.getTrendScore() != null ? result.getTrendScore() : 0.0));
            existing.setVelocity(result.getVelocity() != null ? result.getVelocity() : existing.getVelocity());
            existing.setConfidence(result.getConfidence() != null ? result.getConfidence() : existing.getConfidence());
            existing.setAiExplanation(result.getExplanation());
            existing.setCategory(result.getCategory());

            // Update R3 fields if provided
            if (result.getBrandMention() != null)
                existing.setBrandMention(result.getBrandMention());
            if (result.getPricePoint() != null)
                existing.setPricePoint(result.getPricePoint());
            if (result.getGender() != null)
                existing.setGender(result.getGender());
            if (result.getIndiaRelevant() != null)
                existing.setIndiaRelevant(result.getIndiaRelevant());

            return existing;
        } else {
            // Create new trend
            return Trend.builder()
                    .productName(result.getProductName())
                    .category(result.getCategory())
                    .trendScore(result.getTrendScore() != null ? result.getTrendScore() : 0.0)
                    .velocity(result.getVelocity() != null ? result.getVelocity() : 0.0)
                    .confidence(result.getConfidence() != null ? result.getConfidence() : 0.0)
                    .aiExplanation(result.getExplanation())
                    .audienceTag(result.getAudienceTag() != null ? result.getAudienceTag() : "Gen-Z")
                    .brandMention(result.getBrandMention())
                    .pricePoint(result.getPricePoint())
                    .gender(result.getGender())
                    .indiaRelevant(result.getIndiaRelevant() != null ? result.getIndiaRelevant() : false)
                    .mentionCountThisWeek(1)
                    .mentionCountLastWeek(0)
                    .build();
        }
    }

    /**
     * Check similarity: containment OR Levenshtein distance < 4
     */
    private boolean isSimilar(String a, String b) {
        if (a.isEmpty() || b.isEmpty())
            return false;
        // Containment check
        if (a.contains(b) || b.contains(a))
            return true;
        // Levenshtein distance check
        return levenshteinDistance(a, b) < 4;
    }

    /**
     * Normalize: lowercase, trim, remove special characters
     */
    private String normalize(String name) {
        if (name == null)
            return "";
        return name.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ");
    }

    /**
     * Standard Levenshtein distance calculation
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++)
            dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }
}
