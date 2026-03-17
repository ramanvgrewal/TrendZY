package com.trendzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendzy.config.GroqConfig;
import com.trendzy.model.mongo.Signal;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.mongo.SignalRepository;
import com.trendzy.repository.mongo.TrendRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiAnalysisService {

    private final ChatClient chatClient;
    private final TokenBudgetService tokenBudgetService;
    private final GroqConfig groqConfig;
    private final TrendRepository trendRepository;
    private final SignalRepository signalRepository;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(ChatClient.Builder builder,
                             TokenBudgetService tokenBudgetService,
                             GroqConfig groqConfig,
                             TrendRepository trendRepository,
                             SignalRepository signalRepository,
                             ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.tokenBudgetService = tokenBudgetService;
        this.groqConfig = groqConfig;
        this.trendRepository = trendRepository;
        this.signalRepository = signalRepository;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public void analyzeSignalsBatch(List<Signal> signals) {
        if (signals == null || signals.isEmpty()) {
            log.info("[AI] No signals provided for analysis — skipping batch");
            return;
        }

        log.info("[AI] Starting batch analysis for {} signals", signals.size());

        int estimatedTokens = signals.size() * 1000 + 1500;
        log.debug("[AI] Estimated token cost for this batch: {}", estimatedTokens);

        if (!tokenBudgetService.hasEnoughBudget(estimatedTokens)) {
            log.warn("[AI] Insufficient token budget — needed: {}, skipping batch", estimatedTokens);
            return;
        }

        try {
            String promptText = buildPrompt(signals);
            log.debug("[AI] Prompt built ({} chars), sending to Groq...", promptText.length());

            ChatResponse response = chatClient
                    .prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String responseContent = response.getResult().getOutput().getText();
            log.debug("[AI] Raw Groq response ({} chars):\n{}",
                    responseContent.length(), responseContent);

            int tokensUsed = (response.getMetadata() != null
                    && response.getMetadata().getUsage() != null)
                    ? response.getMetadata().getUsage().getTotalTokens().intValue()
                    : estimatedTokens;

            log.info("[AI] Groq responded — tokens used: {}", tokensUsed);

            processAiResponse(responseContent, signals);

            tokenBudgetService.deductTokens(tokensUsed, true, true);
            log.info("[AI] Batch analysis complete. Tokens deducted: {}", tokensUsed);

        } catch (Exception e) {
            log.error("[AI] Batch analysis failed — deducting estimated {} tokens. Error: {}",
                    estimatedTokens, e.getMessage(), e);
            tokenBudgetService.deductTokens(estimatedTokens, true, false);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PROMPT BUILDER
    // ─────────────────────────────────────────────────────────────

    private String buildPrompt(List<Signal> signals) {
        log.debug("[AI] Building prompt for {} signals", signals.size());

        StringBuilder sb = new StringBuilder();
        sb.append("You are a trend intelligence engine for Indian Gen-Z consumers.\n");
        sb.append("Analyze these social media signals and detect ONLY real buyable consumer products — not brand names alone, memes, or generic discussions.\n\n");
        sb.append("ALLOWED product types: clothes, streetwear, sneakers, shoes, jackets, hoodies, wearables, watches, earbuds, headphones, gadgets, skincare, beauty products, accessories (e.g. oversized hoodie, Nike Dunk sneakers, Apple AirPods, Casio watch).\n");
        sb.append("REJECT: brand mentions without a product, memes, company names without a specific product, generic lifestyle discussions.\n\n");
        sb.append("Output STRICTLY as a valid JSON array. No markdown. No backticks. No explanation. Just the JSON array.\n");
        sb.append("Each object in the array must have EXACTLY these fields:\n");
        sb.append("- productName (string): specific product or style name\n");
        sb.append("- category (string): e.g. Fashion, Beauty, Skincare, Footwear\n");
        sb.append("- subcategory (string): e.g. Dresses, Lip Products, Sneakers\n");
        sb.append("- trendScore (integer 0-100): based on signal volume and buzz strength\n");
        sb.append("- velocity (integer): estimated growth percentage as a number e.g. 38\n");
        sb.append("- velocityLabel (string): formatted as '+38%' or '-5%'\n");
        sb.append("- tier (string): 'trending' if trendScore > 70, else 'rising'\n");
        sb.append("- vibeTags (array of strings): 2-4 tags each starting with # e.g. ['#Y2K','#Streetwear']\n");
        sb.append("- aiSummary (string): 2-3 sentence summary of why this is trending\n");
        sb.append("- whyTrending (array of strings): 2-3 short specific reasons\n");
        sb.append("- indiaRelevanceNote (string): 1 sentence on India-specific relevance\n");
        sb.append("- estimatedPrice (number): price in INR, 0 if unknown\n\n");
        sb.append("Signals to analyze:\n");
        sb.append("─────────────────────────────────────\n");

        for (int i = 0; i < signals.size(); i++) {
            Signal s = signals.get(i);
            sb.append(String.format("[%d] Source: %s | Subreddit: %s\n",
                    i + 1,
                    s.getSource() != null ? s.getSource() : "unknown",
                    s.getSubreddit() != null ? s.getSubreddit() : "unknown"));
            sb.append("Content: ")
                    .append(s.getContent() != null
                            ? s.getContent().substring(0, Math.min(s.getContent().length(), 300))
                            : "")
                    .append("\n\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // RESPONSE PROCESSOR
    // ─────────────────────────────────────────────────────────────

    private void processAiResponse(String jsonString, List<Signal> signals) throws Exception {
        log.info("[AI] Processing AI response...");

        jsonString = jsonString
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)```\\s*$", "")
                .trim();

        log.debug("[AI] Cleaned JSON string (first 500 chars): {}",
                jsonString.substring(0, Math.min(jsonString.length(), 500)));

        JsonNode root;
        try {
            root = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.error("[AI] Failed to parse JSON response from Groq. Raw content:\n{}", jsonString);
            throw e;
        }

        if (!root.isArray()) {
            log.error("[AI] Expected JSON array from Groq but got: {}", root.getNodeType());
            return;
        }

        log.info("[AI] Groq returned {} trend objects to process", root.size());

        List<String> detectedSubreddits = signals.stream()
                .map(Signal::getSubreddit)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        log.debug("[AI] Detected subreddits in batch: {}", detectedSubreddits);

        int savedCount = 0;
        int skippedCount = 0;

        for (JsonNode node : root) {
            String productName = node.path("productName").asText("").trim();

            if (productName.isEmpty()) {
                log.warn("[AI] Skipping trend with empty productName");
                skippedCount++;
                continue;
            }

            if (trendRepository.existsByProductNameIgnoreCase(productName)) {
                log.debug("[AI] Trend '{}' already exists — skipping", productName);
                skippedCount++;
                continue;
            }

            try {
                Trend trend = buildTrend(node, productName, signals, detectedSubreddits);
                trendRepository.save(trend);
                savedCount++;
                log.info("[AI] ✅ Saved new trend: '{}' | tier: {} | score: {} | price: ₹{}",
                        trend.getProductName(),
                        trend.getTier(),
                        trend.getTrendScore(),
                        trend.getEstimatedPrice());
            } catch (Exception e) {
                log.error("[AI] Failed to save trend '{}': {}", productName, e.getMessage());
                skippedCount++;
            }
        }

        log.info("[AI] Trends saved: {} | skipped/duplicate: {}", savedCount, skippedCount);

        int markedProcessed = 0;
        for (Signal s : signals) {
            s.setProcessed(true);
            signalRepository.save(s);
            markedProcessed++;
        }
        log.info("[AI] Marked {} signals as processed", markedProcessed);
    }

    // ─────────────────────────────────────────────────────────────
    // TREND BUILDER
    // ─────────────────────────────────────────────────────────────

    private Trend buildTrend(JsonNode node,
                             String productName,
                             List<Signal> signals,
                             List<String> detectedSubreddits) {

        long signalCount = signals.stream()
                .filter(s -> s.getContent() != null &&
                        s.getContent().toLowerCase()
                                .contains(productName.toLowerCase()))
                .count();
        long finalSignalCount = Math.max(signalCount, 1);
        log.debug("[AI] Signal count for '{}': {}", productName, finalSignalCount);

        List<String> vibeTags = new ArrayList<>();
        node.path("vibeTags").forEach(tag -> {
            String tagText = tag.asText().trim();
            if (!tagText.isEmpty()) vibeTags.add(tagText);
        });

        List<String> whyTrending = new ArrayList<>();
        node.path("whyTrending").forEach(reason -> {
            String reasonText = reason.asText().trim();
            if (!reasonText.isEmpty()) whyTrending.add(reasonText);
        });

        // imageUrl, amazonUrl, myntraUrl, flipkartUrl are set only by ProductEnrichmentService
        // after extracting real product images from ecommerce product pages (no stock/placeholder images)

        return Trend.builder()
                .productName(productName)
                .category(node.path("category").asText("Fashion"))
                .subcategory(node.path("subcategory").asText(""))
                .trendScore(node.path("trendScore").asDouble(50.0))
                .velocity(node.path("velocity").asDouble(0.0))
                .velocityLabel(node.path("velocityLabel").asText("+0%"))
                .tier(node.path("tier").asText("rising"))
                .vibeTags(vibeTags)
                .aiSummary(node.path("aiSummary").asText(""))
                .whyTrending(whyTrending)
                .indiaRelevanceNote(node.path("indiaRelevanceNote").asText(""))
                .totalSignals(finalSignalCount)
                .detectedSubreddits(detectedSubreddits)
                .youtubeVideoCount(0)
                .estimatedPrice(node.path("estimatedPrice").asDouble(0.0))
                // ✅ No imageUrl, no amazonUrl, no myntraUrl, no flipkartUrl
                // These are set by ProductEnrichmentService only
                .platform(null)
                .imageUrl(null)
                .amazonUrl(null)
                .myntraUrl(null)
                .flipkartUrl(null)
                .firstDetectedAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .active(true)
                .build();
    }
}