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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class YouTubeCollectorService {

    private final SignalRepository signalRepository;
    private final RestClient restClient;

    @Value("${youtube.api-key:DISABLED}")
    private String apiKey;

    // Rotate through these queries for broader trend coverage
    private static final List<String> SEARCH_QUERIES = List.of(
            "Indian fashion trends 2025",
            "India streetwear haul",
            "Indian skincare routine",
            "Indian beauty products review",
            "Gen Z fashion India",
            "affordable fashion India",
            "Indian brand review"
    );

    private static final int MAX_CONTENT_LENGTH = 500;

    private static final List<String> BUY_INTENT_KEYWORDS = List.of(
            "buy", "where to buy", "link", "price", "review", "worth it", "ordered", "got this",
            "purchase", "amazon", "myntra", "flipkart", "haul", "unboxing"
    );
    private static final List<String> PRODUCT_KEYWORDS = List.of(
            "hoodie", "sneakers", "watch", "earbuds", "tshirt", "jacket", "skincare",
            "makeup", "gadgets", "shoes", "streetwear", "headphones", "accessories"
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
        // Gracefully skip if YouTube API not configured
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("DISABLED")) {
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
                log.info("[YOUTUBE] Query '{}' → {} new signals", query, count);

                Thread.sleep(1000); // Rate limiting

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
                + "&maxResults=15"
                + "&regionCode=IN"           // India-specific results
                + "&relevanceLanguage=en"
                + "&key=" + apiKey;

        log.debug("[YOUTUBE] Fetching: q={}", query);

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
            log.warn("[YOUTUBE] No items in response for query '{}'", query);
            return 0;
        }

        int collected = 0;

        for (JsonNode item : response.get("items")) {
            try {
                JsonNode idNode = item.path("id");
                String videoId = idNode.path("videoId").asText("");

                if (videoId.isBlank()) continue;

                // Skip duplicates
                if (signalRepository.existsBySourceId(videoId)) {
                    log.debug("[YOUTUBE] Duplicate video {} — skipping", videoId);
                    continue;
                }

                JsonNode snippet = item.path("snippet");
                String title       = snippet.path("title").asText("").trim();
                String description = snippet.path("description").asText("").trim();

                if (title.isBlank()) continue;

                // Truncate description to prevent DB bloat
                String content = (title + " " + description).trim();
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH);
                }

                String channelTitle = snippet.path("channelTitle").asText("");
                List<String> buyKeywords = extractBuyIntentKeywords(content);
                boolean hasProductKeyword = hasProductKeyword(content);
                int priorityScore = (!buyKeywords.isEmpty() || hasProductKeyword) ? 2 : 1;

                Signal signal = Signal.builder()
                        .source("youtube")
                        .sourceId(videoId)
                        .subreddit(null)
                        .content(content)
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

                log.debug("[YOUTUBE] Saved: '{}' by '{}'",
                        title.substring(0, Math.min(title.length(), 60)),
                        channelTitle);

            } catch (Exception e) {
                log.warn("[YOUTUBE] Failed to process video item: {}", e.getMessage());
            }
        }

        return collected;
    }

    private List<String> extractBuyIntentKeywords(String content) {
        if (content == null || content.isBlank()) return List.of();
        String lower = content.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String kw : BUY_INTENT_KEYWORDS) {
            if (lower.contains(kw)) found.add(kw);
        }
        return found;
    }

    private boolean hasProductKeyword(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return PRODUCT_KEYWORDS.stream().anyMatch(lower::contains);
    }
}