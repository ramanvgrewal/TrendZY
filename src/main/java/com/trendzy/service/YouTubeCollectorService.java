package com.trendzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trendzy.model.mongo.Signal;
import com.trendzy.repository.mongo.SignalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YouTubeCollectorService {

    private final SignalRepository signalRepository;
    private final RestClient restClient;

    @Value("${youtube.api-key:DISABLED}")
    private String apiKey;

    /**
     * Search queries — deliberately specific to Indian Gen-Z content.
     *
     * Rationale for each category:
     *   HAUL queries      → product names appear in titles/descriptions with buy links
     *   REVIEW queries    → "worth it", "buy", "don't buy" signals are high-value
     *   BRAND queries     → Indian D2C brands (Snitch, Bewakoof, Urbanic) have active
     *                       YouTube presence; their trending items are India-first
     *   AESTHETIC queries → cottagecore, clean girl, Y2K — these drive search volume
     *                       on Indian e-commerce 2–4 weeks after YouTube peaks
     *   TUTORIAL queries  → beauty/skincare tutorials mention specific products heavily
     */
    private static final List<String> SEARCH_QUERIES = List.of(
            // ── Haul content (highest product density) ──
            "meesho haul 2025",
            "myntra haul india 2025",
            "nykaa haul india",
            "ajio fashion haul india",
            "thrift haul india streetwear",
            "h&m india haul",
            "zara india haul",

            // ── Affordable / budget reviews (high buy-intent) ──
            "affordable sneakers india review",
            "budget skincare india 2025",
            "affordable streetwear india brands",
            "best earbuds under 2000 india",
            "best smartwatch india 2025",

            // ── Indian D2C and indie brands ──
            "snitch clothing review",
            "bewakoof clothing haul",
            "urbanic india haul",
            "indian indie brand review 2025",
            "new indian fashion brand 2025",

            // ── Gen Z aesthetics (trend-leading content) ──
            "y2k fashion india",
            "gen z fashion india 2025",
            "clean girl aesthetic india",
            "indian streetwear style",
            "cottagecore india outfit",

            // ── Beauty and skincare specifics ──
            "indian skincare routine affordable",
            "minimalist skincare india review",
            "korean skincare india 2025",
            "nykaa skincare review",
            "best lip tint india",
            "hyaluronic acid serum india review",
            "vitamin c serum india affordable"
    );

    private static final int MAX_CONTENT_LEN = 600;

    private static final List<String> BUY_INTENT_KEYWORDS = List.of(
            "buy", "where to buy", "link", "price", "review", "worth it",
            "ordered", "purchased", "got this", "amazon", "myntra", "flipkart",
            "meesho", "nykaa", "haul", "unboxing", "₹", "rs ", "discount",
            "affordable", "budget", "under 500", "under 1000", "under 2000"
    );

    private static final List<String> PRODUCT_KEYWORDS = List.of(
            "hoodie", "sneakers", "watch", "earbuds", "tshirt", "jacket", "skincare",
            "makeup", "gadgets", "shoes", "streetwear", "headphones", "accessories",
            "serum", "sunscreen", "lip tint", "moisturizer", "cargo pants", "co-ord",
            "tote bag", "sling bag", "necklace", "earrings", "smartwatch"
    );

    public YouTubeCollectorService(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public int collectSignals() {
        if (apiKey == null || apiKey.isBlank() || "DISABLED".equals(apiKey)) {
            log.info("[YOUTUBE] API key not configured — skipping YouTube collection");
            return 0;
        }

        log.info("[YOUTUBE] Starting collection across {} search queries",
                SEARCH_QUERIES.size());

        int totalCollected = 0;

        for (String query : SEARCH_QUERIES) {
            try {
                int count = collectForQuery(query);
                totalCollected += count;

                if (count > 0) {
                    log.info("[YOUTUBE] '{}' → {} new signals", query, count);
                }

                Thread.sleep(1000); // YouTube Data API quota is per-day, not per-second

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[YOUTUBE] Collection interrupted");
                break;
            } catch (Exception e) {
                log.error("[YOUTUBE] Failed for query '{}': {}", query, e.getMessage());
            }
        }

        log.info("[YOUTUBE] Collection complete — total new signals: {}", totalCollected);
        return totalCollected;
    }

    // ─────────────────────────────────────────────────────────────
    // COLLECT FOR ONE QUERY
    // ─────────────────────────────────────────────────────────────

    private int collectForQuery(String query) {
        String encodedQuery = query.replace(" ", "+");
        String url = "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet"
                + "&q=" + encodedQuery
                + "&type=video"
                + "&maxResults=20"
                + "&regionCode=IN"
                + "&relevanceLanguage=en"
                + "&videoDuration=medium"   // skip shorts <4min (less product detail)
                + "&order=viewCount"        // high-view videos = stronger trend signal
                + "&key=" + apiKey;

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("[YOUTUBE] HTTP error for query '{}': {}", query, e.getMessage());
            return 0;
        }

        if (response == null || !response.has("items")) {
            log.warn("[YOUTUBE] No items for query '{}'", query);
            return 0;
        }

        int collected = 0;

        for (JsonNode item : response.get("items")) {
            try {
                String videoId = item.path("id").path("videoId").asText("").trim();
                if (videoId.isBlank()) continue;
                if (signalRepository.existsBySourceId(videoId)) continue;

                JsonNode snippet     = item.path("snippet");
                String title         = snippet.path("title").asText("").trim();
                String description   = snippet.path("description").asText("").trim();
                String channelTitle  = snippet.path("channelTitle").asText("").trim();

                if (title.isBlank()) continue;

                String combined = (title + " " + description).trim();
                if (combined.length() > MAX_CONTENT_LEN) {
                    combined = combined.substring(0, MAX_CONTENT_LEN);
                }

                List<String> buyKeywords  = extractMatchingKeywords(combined, BUY_INTENT_KEYWORDS);
                boolean hasProductKeyword = hasAnyKeyword(combined, PRODUCT_KEYWORDS);

                int priorityScore;
                if (!buyKeywords.isEmpty() && hasProductKeyword) {
                    priorityScore = 3;
                } else if (!buyKeywords.isEmpty() || hasProductKeyword) {
                    priorityScore = 2;
                } else {
                    priorityScore = 1;
                }

                Signal signal = Signal.builder()
                        .source("youtube")
                        .sourceId(videoId)
                        .subreddit(null)
                        .content(combined)
                        .url("https://youtube.com/watch?v=" + videoId)
                        .upvotes(0)
                        .commentCount(0)
                        .processed(false)
                        .buyIntentKeywords(buyKeywords)
                        .priorityScore(priorityScore)
                        .collectedAt(LocalDateTime.now())
                        .build();

                signalRepository.save(signal);
                collected++;

                log.debug("[YOUTUBE] Saved: '{}' by '{}' | priority={}",
                        title.substring(0, Math.min(title.length(), 60)),
                        channelTitle, priorityScore);

            } catch (Exception e) {
                log.warn("[YOUTUBE] Failed to process video item: {}", e.getMessage());
            }
        }

        return collected;
    }

    // ─────────────────────────────────────────────────────────────
    // KEYWORD HELPERS
    // ─────────────────────────────────────────────────────────────

    private List<String> extractMatchingKeywords(String content, List<String> keywords) {
        if (content == null || content.isBlank()) return List.of();
        String lower = content.toLowerCase();
        return keywords.stream()
                .filter(lower::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean hasAnyKeyword(String content, List<String> keywords) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return keywords.stream().anyMatch(lower::contains);
    }
}