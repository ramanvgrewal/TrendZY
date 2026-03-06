package com.trendpulse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendpulse.dto.AiAnalysisResult;
import com.trendpulse.entity.ProductLink;
import com.trendpulse.entity.RawSignal;
import com.trendpulse.entity.Trend;
import com.trendpulse.repository.ProductLinkRepository;
import com.trendpulse.repository.RawSignalRepository;
import com.trendpulse.repository.TrendRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class AiAnalysisService {

    private final RawSignalRepository rawSignalRepository;
    private final TrendRepository trendRepository;
    private final ProductLinkRepository productLinkRepository;
    private final VelocityService velocityService;
    private final TrendDeduplicationService deduplicationService;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    @Value("${affiliate.amazon-tag}")
    private String amazonTag;

    @Value("${affiliate.myntra-tag}")
    private String myntraTag;

    private static final int BATCH_SIZE = 5;

    public AiAnalysisService(
            RawSignalRepository rawSignalRepository,
            TrendRepository trendRepository,
            ProductLinkRepository productLinkRepository,
            VelocityService velocityService,
            TrendDeduplicationService deduplicationService,
            ObjectMapper objectMapper,
            ChatClient.Builder chatClientBuilder) {
        this.rawSignalRepository = rawSignalRepository;
        this.trendRepository = trendRepository;
        this.productLinkRepository = productLinkRepository;
        this.velocityService = velocityService;
        this.deduplicationService = deduplicationService;
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a trend intelligence analyst. Always respond with valid JSON only.")
                .build();
    }

    /**
     * Analyze unprocessed signals using Groq AI (via Spring AI)
     */
    public int analyzeSignals() {
        List<RawSignal> unprocessed = rawSignalRepository.findUnprocessedSignals();

        if (unprocessed.isEmpty()) {
            log.info("No unprocessed signals to analyze");
            return 0;
        }

        int totalBatches = (int) Math.ceil((double) unprocessed.size() / BATCH_SIZE);
        log.info("🤖 Starting AI analysis of {} unprocessed signals in {} batches", unprocessed.size(), totalBatches);
        int totalProcessed = 0;

        for (int i = 0; i < unprocessed.size(); i += BATCH_SIZE) {
            List<RawSignal> batch = unprocessed.subList(i, Math.min(i + BATCH_SIZE, unprocessed.size()));
            int batchNum = (i / BATCH_SIZE) + 1;

            try {
                List<AiAnalysisResult> results = analyzeBatch(batch);
                processBatchResults(results, batch);
                totalProcessed += batch.size();
                log.info("  ✅ Processed batch {}/{}", batchNum, totalBatches);
            } catch (Exception e) {
                if (e.getMessage() != null
                        && (e.getMessage().contains("429") || e.getMessage().toLowerCase().contains("rate limit"))) {
                    log.warn("  ⚠️ Rate limit hit on batch {}. Waiting 20s before retry...", batchNum);
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    try {
                        List<AiAnalysisResult> retryResults = analyzeBatch(batch);
                        processBatchResults(retryResults, batch);
                        totalProcessed += batch.size();
                        log.info("  ✅ Retry succeeded for batch {}/{}", batchNum, totalBatches);
                    } catch (Exception retryEx) {
                        log.error("  ❌ Retry also failed for batch {}: {}", batchNum, retryEx.getMessage());
                    }
                } else {
                    log.error("  ❌ Failed to analyze batch {}: {}", batchNum, e.getMessage());
                }
            }

            if (i + BATCH_SIZE < unprocessed.size()) {
                log.info("  ⏳ Rate limit pause — waiting 15s before next batch...");
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("📊 AI analysis complete: {} signals processed", totalProcessed);
        return totalProcessed;
    }

    private List<AiAnalysisResult> analyzeBatch(List<RawSignal> signals) {
        StringBuilder signalSummary = new StringBuilder();
        for (int i = 0; i < signals.size(); i++) {
            RawSignal s = signals.get(i);
            signalSummary.append(String.format(
                    "%d. [r/%s] Title: \"%s\" | Comment: \"%s\" | Keyword: \"%s\" | Possible Product: \"%s\"\n",
                    i + 1,
                    s.getSubreddit(),
                    s.getPostTitle() != null ? s.getPostTitle() : "N/A",
                    s.getCommentBody() != null ? truncate(s.getCommentBody(), 200) : "N/A",
                    s.getKeywordMatched(),
                    s.getProductMentioned() != null ? s.getProductMentioned() : "Unknown"));
        }

        String prompt = buildAnalysisPrompt(signalSummary.toString());
        String response = callAi(prompt);
        return parseAnalysisResponse(response);
    }

    private String buildAnalysisPrompt(String signalSummary) {
        return """
                You are a trend intelligence analyst specializing in Gen-Z consumer behavior and emerging product trends, with a focus on the Indian market.

                Analyze these Reddit signals that contain buy-intent keywords. Your job is to:
                1. Identify distinct products or product categories being discussed
                2. Score each trend based on signal strength
                3. Determine if this is a Gen-Z trend, mainstream, or niche
                4. Identify brand names, price segments, and gender targeting

                Here are the signals:

                %s

                For each distinct product/trend you identify, return a JSON array with objects containing:
                - "productName": The clear, concise product name (e.g., "Stanley Quencher Tumbler", "Baggy Cargo Pants")
                - "trendScore": A score from 1-10 indicating trend strength (10 = viral, 1 = barely noticed)
                - "velocity": A number from 0-100 indicating how fast this trend is growing
                - "explanation": A 2-3 sentence explanation of WHY this is trending, written in an engaging style
                - "category": One of: "Fashion", "Tech", "Beauty", "Home", "Food", "Fitness", "Gaming", "Lifestyle", "Other"
                - "confidence": A score from 0-1 indicating how confident you are in this analysis
                - "audienceTag": One of: "Gen-Z", "Millennial", "Universal", "Niche"
                - "brandMention": The specific brand name if mentioned (e.g., "Nike", "Samsung"), or null if no brand
                - "pricePoint": One of: "budget", "mid", "premium" — based on context clues about the product's price
                - "gender": One of: "male", "female", "unisex" — based on the target audience
                - "indiaRelevant": true if the product is available or relevant in India, false otherwise

                IMPORTANT: Respond ONLY with a valid JSON array. No markdown, no explanation outside the JSON.
                If no clear trends are found, return an empty array: []

                Example response format:
                [
                  {
                    "productName": "Stanley Quencher",
                    "trendScore": 8.5,
                    "velocity": 75,
                    "explanation": "The Stanley Quencher tumbler has become a Gen-Z status symbol...",
                    "category": "Lifestyle",
                    "confidence": 0.9,
                    "audienceTag": "Gen-Z",
                    "brandMention": "Stanley",
                    "pricePoint": "mid",
                    "gender": "unisex",
                    "indiaRelevant": false
                  }
                ]
                """
                .formatted(signalSummary);
    }

    private String callAi(String prompt) {
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response != null)
                return response;

            log.error("AI returned null response");
            return "[]";
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage());
            throw e; // re-throw so retry logic in analyzeSignals can catch it
        }
    }

    private List<AiAnalysisResult> parseAnalysisResponse(String response) {
        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("```json"))
                cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```"))
                cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```"))
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, new TypeReference<List<AiAnalysisResult>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            log.debug("Raw response: {}", response);
            return Collections.emptyList();
        }
    }

    private void processBatchResults(List<AiAnalysisResult> results, List<RawSignal> signals) {
        for (AiAnalysisResult result : results) {
            if (result.getProductName() == null || result.getProductName().isBlank())
                continue;

            // R4: Use deduplication service to find or create trend
            Trend trend = deduplicationService.findOrCreateTrend(result);

            // Apply velocity label
            velocityService.calculateAndApplyVelocity(trend);
            trendRepository.save(trend);

            // Generate affiliate links for new trends only
            if (trend.getMentionCountThisWeek() <= 1 && trend.getProductLinks().isEmpty()) {
                generateAffiliateLinks(trend);
            }
        }

        // Mark signals as processed
        for (RawSignal signal : signals) {
            signal.setProcessed(true);
            rawSignalRepository.save(signal);
        }
    }

    public void generateAffiliateLinks(Trend trend) {
        String plusName = sanitizeProductName(trend.getProductName(), "+");
        String hyphenName = sanitizeProductName(trend.getProductName(), "-");

        ProductLink amazonLink = ProductLink.builder()
                .trend(trend)
                .platform("Amazon")
                .affiliateUrl(String.format("https://www.amazon.in/s?k=%s&tag=%s", plusName, amazonTag))
                .priceRange("Varies")
                .build();
        productLinkRepository.save(amazonLink);

        ProductLink myntraLink = ProductLink.builder()
                .trend(trend)
                .platform("Myntra")
                .affiliateUrl(String.format("https://www.myntra.com/%s", hyphenName))
                .priceRange("Varies")
                .build();
        productLinkRepository.save(myntraLink);

        ProductLink flipkartLink = ProductLink.builder()
                .trend(trend)
                .platform("Flipkart")
                .affiliateUrl(String.format("https://www.flipkart.com/search?q=%s&affid=%s", plusName, myntraTag))
                .priceRange("Varies")
                .build();
        productLinkRepository.save(flipkartLink);
    }

    /**
     * Sanitize product name: lowercase, trim, remove special chars, replace spaces
     * with separator.
     */
    private String sanitizeProductName(String name, String separator) {
        if (name == null)
            return "";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", separator);
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
