package com.trendzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RedditCollectorService {

    private final SignalRepository signalRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${reddit.user-agent}")
    private String userAgent;

    @Value("${reddit.subreddits}")
    private String subredditsRaw; // comma-separated string — safe with @Value

    private static final List<String> BUY_INTENT_KEYWORDS = List.of(
            "where to buy", "where can i buy", "how to buy", "link to buy",
            "buy this", "want to buy", "looking to buy", "want this",
            "where is this from", "where did you get", "source?", "source please",
            "how much", "price?", "available on", "amazon link", "myntra",
            "flipkart", "ordered this", "just bought", "purchased",
            "shopping for", "worth buying", "should i buy", "is it worth",
            "good quality", "dupe of", "affordable", "budget option",
            "where to get", "recommend", "suggestions", "link", "review", "worth it", "got this"
    );

    private static final List<String> PRODUCT_CATEGORY_KEYWORDS = List.of(
            "hoodie", "sneakers", "watch", "earbuds", "tshirt", "t-shirt", "jacket",
            "skincare", "makeup", "gadgets", "shoes", "streetwear", "headphones",
            "accessories", "wearables", "beauty", "lipstick", "serum", "sneaker"
    );

    // Minimum quality thresholds
    private static final int MIN_UPVOTES = 2;
    private static final int MAX_CONTENT_LENGTH = 1000; // chars

    public RedditCollectorService(SignalRepository signalRepository,
                                  ObjectMapper objectMapper) {
        this.signalRepository = signalRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public int collectSignals() {
        List<String> subreddits = Arrays.stream(subredditsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        log.info("[REDDIT] Starting collection from {} subreddits", subreddits.size());

        int totalCollected = 0;

        for (String sub : subreddits) {
            try {
                int count = collectFromSubreddit(sub);
                totalCollected += count;
                log.info("[REDDIT] r/{} → {} new signals collected", sub, count);

                // Rate limit — be polite to Reddit API
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[REDDIT] Collection interrupted");
                break;
            } catch (Exception e) {
                log.error("[REDDIT] Failed to collect from r/{}: {}", sub, e.getMessage());
            }
        }

        log.info("[REDDIT] Collection complete — total new signals: {}", totalCollected);
        return totalCollected;
    }

    // ─────────────────────────────────────────────────────────────
    // COLLECT FROM ONE SUBREDDIT
    // ─────────────────────────────────────────────────────────────

    private int collectFromSubreddit(String subreddit) {
        String url = "https://www.reddit.com/r/" + subreddit + "/new.json?limit=100";

        log.debug("[REDDIT] Fetching: {}", url);

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(url)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("[REDDIT] HTTP error for r/{}: {}", subreddit, e.getMessage());
            return 0;
        }

        if (response == null
                || !response.has("data")
                || !response.get("data").has("children")) {
            log.warn("[REDDIT] Unexpected response structure for r/{}", subreddit);
            return 0;
        }

        JsonNode children = response.get("data").get("children");
        int collected = 0;

        for (JsonNode child : children) {
            try {
                JsonNode data = child.path("data");
                String sourceId = data.path("id").asText("");

                if (sourceId.isBlank()) continue;

                // Skip duplicates
                if (signalRepository.existsBySourceId(sourceId)) {
                    log.debug("[REDDIT] Duplicate signal {} — skipping", sourceId);
                    continue;
                }

                // Quality filter — skip low-engagement posts
                long upvotes = data.path("score").asLong(0);
                if (upvotes < MIN_UPVOTES) {
                    log.debug("[REDDIT] Low upvotes ({}) for {} — skipping", upvotes, sourceId);
                    continue;
                }

                String title    = data.path("title").asText("").trim();
                String selftext = data.path("selftext").asText("").trim();

                // Skip deleted/removed posts
                if (selftext.equals("[deleted]") || selftext.equals("[removed]")) {
                    selftext = "";
                }

                // Truncate to prevent DB bloat
                String content = (title + " " + selftext).trim();
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH);
                }

                // Skip if no meaningful content
                if (title.isBlank()) continue;

                List<String> buyKeywords = extractBuyIntentKeywords(content);
                boolean hasProductKeyword = hasProductCategoryKeyword(content);
                int priorityScore = (!buyKeywords.isEmpty() || hasProductKeyword) ? 2 : 1;

                Signal signal = Signal.builder()
                        .source("reddit")
                        .sourceId(sourceId)
                        .subreddit(subreddit)
                        .content(content)
                        .url("https://reddit.com" + data.path("permalink").asText())
                        .upvotes(upvotes)
                        .commentCount(data.path("num_comments").asLong(0))
                        .processed(false)
                        .buyIntentKeywords(buyKeywords)
                        .priorityScore(priorityScore)
                        .collectedAt(LocalDateTime.now())
                        .build();

                signalRepository.save(signal);
                collected++;

                if (!buyKeywords.isEmpty()) {
                    log.debug("[REDDIT] High-intent signal saved: '{}' | keywords: {}",
                            title.substring(0, Math.min(title.length(), 60)),
                            buyKeywords);
                }

            } catch (Exception e) {
                log.warn("[REDDIT] Failed to process post in r/{}: {}", subreddit, e.getMessage());
            }
        }

        return collected;
    }

    // ─────────────────────────────────────────────────────────────
    // BUY INTENT EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private List<String> extractBuyIntentKeywords(String content) {
        if (content == null || content.isBlank()) return List.of();
        String lower = content.toLowerCase();
        return BUY_INTENT_KEYWORDS.stream()
                .filter(lower::contains)
                .collect(Collectors.toList());
    }

    private boolean hasProductCategoryKeyword(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return PRODUCT_CATEGORY_KEYWORDS.stream().anyMatch(lower::contains);
    }
}