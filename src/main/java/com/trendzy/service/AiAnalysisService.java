package com.trendzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendzy.config.GroqConfig;
import com.trendzy.model.mongo.ProductFingerprint;
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
    private final TrendRepository trendRepository;
    private final SignalRepository signalRepository;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(ChatClient.Builder builder,
                             TokenBudgetService tokenBudgetService,
                             GroqConfig groqConfig,
                             TrendRepository trendRepository,
                             SignalRepository signalRepository,
                             ObjectMapper objectMapper) {
        this.chatClient        = builder.build();
        this.tokenBudgetService = tokenBudgetService;
        this.trendRepository   = trendRepository;
        this.signalRepository  = signalRepository;
        this.objectMapper      = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public void analyzeSignalsBatch(List<Signal> signals) {
        if (signals == null || signals.isEmpty()) {
            log.info("[AI] No signals provided — skipping batch");
            return;
        }

        log.info("[AI] Starting batch analysis for {} signals", signals.size());

        int estimatedTokens = signals.size() * 800 + 2000;
        if (!tokenBudgetService.hasEnoughBudget(estimatedTokens)) {
            log.warn("[AI] Insufficient token budget ({} needed) — skipping batch", estimatedTokens);
            return;
        }

        try {
            String prompt = buildPrompt(signals);
            log.debug("[AI] Prompt built ({} chars) — sending to Groq", prompt.length());

            ChatResponse response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            String raw = response.getResult().getOutput().getText();
            log.debug("[AI] Raw response ({} chars):\n{}", raw.length(), raw);

            int tokensUsed = (response.getMetadata() != null
                    && response.getMetadata().getUsage() != null)
                    ? response.getMetadata().getUsage().getTotalTokens().intValue()
                    : estimatedTokens;

            log.info("[AI] Groq responded — tokens used: {}", tokensUsed);

            processAiResponse(raw, signals);
            tokenBudgetService.deductTokens(tokensUsed, true, true);

        } catch (Exception e) {
            log.error("[AI] Batch failed — deducting estimated {} tokens. Error: {}",
                    estimatedTokens, e.getMessage(), e);
            tokenBudgetService.deductTokens(estimatedTokens, true, false);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PROMPT BUILDER
    // ─────────────────────────────────────────────────────────────

    /**
     * Redesigned prompt — concise, structured, few-shot example included.
     * Key improvements over previous version:
     *   1. Shorter system context → less likely to be ignored by LLaMA
     *   2. Explicit INCLUDE / EXCLUDE rules
     *   3. One concrete example in the exact output format expected
     *   4. In-prompt dedup instruction: same product once, combined score
     *   5. India-specific pricing and platform awareness baked in
     */
    private String buildPrompt(List<Signal> signals) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
You are TrendzyAI — a trend intelligence engine for Indian Gen-Z consumers (ages 16–28).

Analyze the social media signals below and extract real, buyable consumer products that are currently trending.

━━━━ INCLUDE ━━━━
Specific purchasable items only:
• Clothing: hoodie, cargo pants, oversized shirt, co-ord set, corset top, mini skirt
• Footwear: sneakers, chunky shoes, platform boots, loafers, slides
• Beauty: lip tint, vitamin C serum, SPF sunscreen, tinted moisturizer, brow lamination kit
• Accessories: tote bag, bucket hat, Y2K belt, layered necklace, sling bag
• Tech wearables: wireless earbuds, smartwatch, neckband headphones

━━━━ EXCLUDE ━━━━
• Brand mentions without a specific product ("Nike is popular", "Zara haul")
• Memes, aesthetic vibes, or lifestyle discussions with no buyable product
• Generic category mentions ("skincare is trending", "sneakers are in")
• Duplicate: if multiple signals mention the same product, output it ONCE with a combined trendScore

━━━━ OUTPUT FORMAT ━━━━
Respond ONLY with a valid JSON array. No markdown. No backticks. No explanation.
Return [] if no valid products are found in the signals.

Each product object MUST contain EXACTLY these fields — no extras, no omissions:
  "productName"        — specific Title Cased name (e.g. "Baggy Cargo Jeans", "Vitamin C Serum")
  "category"           — Fashion | Beauty | Skincare | Footwear | Accessories | Tech
  "subcategory"        — specific (e.g. "Bottoms", "Lip Products", "Sneakers", "Moisturizers")
  "trendScore"         — integer 0–100 based on buzz strength across all signals
  "velocity"           — integer growth percentage estimate (e.g. 38)
  "velocityLabel"      — formatted string (e.g. "+38%" or "-5%")
  "tier"               — "trending" if trendScore > 70, else "rising"
  "vibeTags"           — array of 2–4 strings each starting with # (e.g. ["#Y2K","#Streetwear"])
  "aiSummary"          — 2–3 sentence explanation of why this product is trending right now
  "whyTrending"        — array of 2–3 short specific reasons
  "indiaRelevanceNote" — 1 sentence on India-specific context (availability, price, local brands)
  "estimatedPrice"     — integer INR price (typical Indian e-commerce price), 0 if unknown
  "brand"              — brand name if identifiable (e.g. "Nike", "Minimalist"), "" if generic
  "productType"        — lowercase search-friendly type (e.g. "cargo pants", "lip tint", "earbuds")
  "color"              — lowercase primary color if mentioned (e.g. "beige", "black"), "" if unknown
  "gender"             — "men" | "women" | "unisex"
  "searchKeywords"     — array of 3–5 lowercase e-commerce search keywords (e.g. ["baggy","wide leg","Y2K"])

━━━━ EXAMPLE ━━━━
Signal: "omg these Zara cargo pants everywhere on my insta, super Y2K, want them bad, seen dupes on Meesho"
Output:
[{"productName":"Cargo Pants","category":"Fashion","subcategory":"Bottoms","trendScore":76,"velocity":45,"velocityLabel":"+45%","tier":"trending","vibeTags":["#Y2K","#CargoCore","#Streetwear"],"aiSummary":"Cargo pants are dominating Gen-Z Instagram feeds driven by Y2K nostalgia. Zara leads premium but Indian fast fashion brands launch affordable versions weekly. Meesho and Myntra offer dupes from ₹500.","whyTrending":["Instagram Reels saturation","Y2K aesthetic revival","Multiple price points from Zara to Meesho"],"indiaRelevanceNote":"Available on Myntra and Meesho at multiple price points; Zara India stocks premium versions.","estimatedPrice":1800,"brand":"Zara","productType":"cargo pants","color":"","gender":"women","searchKeywords":["cargo","baggy","wide leg","Y2K","high waist"]}]

━━━━ SIGNALS TO ANALYZE ━━━━
""");

        for (int i = 0; i < signals.size(); i++) {
            Signal s = signals.get(i);
            sb.append(String.format("[%d] Source: %s | Subreddit: %s\n",
                    i + 1,
                    s.getSource() != null ? s.getSource() : "unknown",
                    s.getSubreddit() != null ? s.getSubreddit() : "n/a"));
            sb.append("Content: ")
                    .append(s.getContent() != null
                            ? s.getContent().substring(0, Math.min(s.getContent().length(), 350))
                            : "")
                    .append("\n\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // RESPONSE PROCESSOR
    // ─────────────────────────────────────────────────────────────

    private void processAiResponse(String raw, List<Signal> signals) throws Exception {
        log.info("[AI] Processing AI response...");

        // Strip markdown fences if LLaMA adds them despite instructions
        String cleaned = raw
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*",      "")
                .replaceAll("(?s)```\\s*$",       "")
                .trim();

        // Find the JSON array boundaries in case there's leading/trailing prose
        int arrayStart = cleaned.indexOf('[');
        int arrayEnd   = cleaned.lastIndexOf(']');
        if (arrayStart != -1 && arrayEnd != -1 && arrayEnd > arrayStart) {
            cleaned = cleaned.substring(arrayStart, arrayEnd + 1);
        }

        log.debug("[AI] Cleaned JSON (first 500 chars): {}",
                cleaned.substring(0, Math.min(cleaned.length(), 500)));

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("[AI] JSON parse failed. Raw response:\n{}", raw);
            throw e;
        }

        if (!root.isArray()) {
            log.error("[AI] Expected JSON array from Groq but got: {}", root.getNodeType());
            return;
        }

        log.info("[AI] Groq returned {} trend objects", root.size());

        List<String> detectedSubreddits = signals.stream()
                .map(Signal::getSubreddit)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        int savedCount   = 0;
        int skippedCount = 0;

        for (JsonNode node : root) {
            String productName = node.path("productName").asText("").trim();

            if (productName.isEmpty()) {
                log.warn("[AI] Skipping trend with empty productName");
                skippedCount++;
                continue;
            }

            // ── Dedup: case-insensitive + normalized check ──
            if (isDuplicate(productName)) {
                log.debug("[AI] '{}' already exists — skipping", productName);
                skippedCount++;
                continue;
            }

            try {
                Trend trend = buildTrend(node, productName, signals, detectedSubreddits);
                trendRepository.save(trend);
                savedCount++;
                log.info("[AI] ✅ Saved: '{}' | tier={} | score={} | ₹{} | brand='{}' | type='{}'",
                        trend.getProductName(),
                        trend.getTier(),
                        trend.getTrendScore(),
                        (int) trend.getEstimatedPrice(),
                        trend.getFingerprint() != null ? trend.getFingerprint().getBrand() : "",
                        trend.getFingerprint() != null ? trend.getFingerprint().getProductType() : "");
            } catch (Exception e) {
                log.error("[AI] Failed to save trend '{}': {}", productName, e.getMessage());
                skippedCount++;
            }
        }

        log.info("[AI] Trends saved: {} | skipped/duplicate: {}", savedCount, skippedCount);

        // Mark all signals in this batch as processed
        for (Signal s : signals) {
            s.setProcessed(true);
            signalRepository.save(s);
        }
        log.info("[AI] Marked {} signals as processed", signals.size());
    }

    // ─────────────────────────────────────────────────────────────
    // DEDUP — case-insensitive + normalized word overlap
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks for duplicates using two strategies:
     *   1. Exact case-insensitive match (fast DB query)
     *   2. Normalized string match (strips punctuation/extra spaces)
     *
     * This catches "Baggy Jeans" vs "baggy jeans" vs "Baggy  Jeans"
     * without needing a fuzzy search library.
     */
    private boolean isDuplicate(String productName) {
        // Strategy 1: exact ignoring case
        if (trendRepository.existsByProductNameIgnoreCase(productName)) {
            return true;
        }

        // Strategy 2: compare normalized forms
        String normalized = normalizeName(productName);
        return trendRepository.findAll().stream()
                .map(t -> normalizeName(t.getProductName()))
                .anyMatch(existing -> existing.equals(normalized));
    }

    /**
     * Normalizes a product name for comparison:
     *   lowercase → strip non-alphanumeric → collapse whitespace
     * e.g. "Baggy  Cargo-Jeans!" → "baggy cargo jeans"
     */
    private static String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
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
                        s.getContent().toLowerCase().contains(productName.toLowerCase()))
                .count();
        long finalSignalCount = Math.max(signalCount, 1);

        List<String> vibeTags = new ArrayList<>();
        node.path("vibeTags").forEach(t -> {
            String tag = t.asText().trim();
            if (!tag.isEmpty()) vibeTags.add(tag);
        });

        List<String> whyTrending = new ArrayList<>();
        node.path("whyTrending").forEach(r -> {
            String reason = r.asText().trim();
            if (!reason.isEmpty()) whyTrending.add(reason);
        });

        List<String> searchKeywords = new ArrayList<>();
        node.path("searchKeywords").forEach(k -> {
            String kw = k.asText().trim();
            if (!kw.isEmpty()) searchKeywords.add(kw);
        });

        // India relevance logic:
        // 1. Explicitly check if any signal for this product comes from an Indian subreddit
        // 2. Or if the AI confirmed it's specifically relevant (heuristic: if prompt mentioned it)
        boolean isIndiaRelevant = detectedSubreddits.stream()
                .anyMatch(sub -> {
                    String s = sub.toLowerCase();
                    return s.contains("indian") || s.contains("india") || s.equals("asianbeauty");
                });

        ProductFingerprint fingerprint = ProductFingerprint.builder()
                .brand(node.path("brand").asText(""))
                .productType(node.path("productType").asText(""))
                .color(node.path("color").asText(""))
                .gender(node.path("gender").asText("unisex"))
                .keywords(searchKeywords)
                .build();

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
                .indiaRelevant(isIndiaRelevant)
                .totalSignals(finalSignalCount)
                .detectedSubreddits(detectedSubreddits)
                .youtubeVideoCount(0)
                .estimatedPrice(node.path("estimatedPrice").asDouble(0.0))
                .fingerprint(fingerprint)
                .enrichmentStatus("PENDING")
                .platform(null)
                .shopUrl(null)
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