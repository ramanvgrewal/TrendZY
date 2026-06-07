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
    private String subredditsRaw;

    // ─────────────────────────────────────────────────────────────
    // SIGNAL QUALITY CONSTANTS
    // ─────────────────────────────────────────────────────────────

    private static final int MIN_UPVOTES      = 2;
    private static final int MAX_CONTENT_LEN  = 1000;

    /**
     * High-intent keywords: signals containing these are 3x more likely
     * to reference a specific buyable product.
     */
    private static final List<String> BUY_INTENT_KEYWORDS = List.of(
            // Purchase intent
            "where to buy", "where can i buy", "how to buy", "link to buy",
            "want to buy", "looking to buy", "where to get", "where did you get",
            "where is this from", "just bought", "purchased", "ordered this",
            "should i buy", "worth buying", "is it worth", "worth it",
            // Product discovery
            "source?", "source please", "link?", "amazon link", "myntra link",
            "available on", "shopping for", "recommend", "suggestions", "review",
            "dupe of", "affordable", "budget option", "affordable version",
            "good quality", "got this", "haul", "unboxing",
            // Price signals
            "how much", "price?", "₹", "rs ", "inr",
            // Platform mentions (high purchase signal)
            "myntra", "flipkart", "meesho", "nykaa", "amazon", "ajio", "bewakoof",
            "snitch", "urbanic", "h&m india", "zara india"
    );

    /**
     * Product category keywords: used for priority scoring.
     * Signals with these are more likely to contain a specific product to analyze.
     */
    private static final List<String> PRODUCT_KEYWORDS = List.of(
            // Clothing
            "hoodie", "hoodie", "oversized", "co-ord", "coord", "cargo pants", "baggy jeans",
            "crop top", "shirt", "t-shirt", "tshirt", "jacket", "blazer", "cardigan",
            "kurta", "lehenga", "ethnic", "streetwear", "streetstyle", "grunge", "y2k",
            "cottagecore", "aesthetic", "thrift", "vintage", "indie",
            // Footwear
            "sneakers", "sneaker", "shoes", "boots", "loafers", "chunky", "platform shoes",
            "nike dunk", "new balance", "puma", "campus shoes",
            // Beauty & Skincare
            "skincare", "serum", "sunscreen", "spf", "moisturizer", "lip tint", "lipstick",
            "foundation", "concealer", "blush", "eyeshadow", "eyeliner", "mascara",
            "brow lamination", "face wash", "toner", "retinol", "niacinamide", "hyaluronic",
            "vitamin c", "cleanser", "exfoliant", "aha", "bha",
            // Accessories & Tech
            "earbuds", "headphones", "watch", "smartwatch", "tote bag", "sling bag",
            "bucket hat", "cap", "sunglasses", "belt bag", "crossbody", "jewelry",
            "necklace", "earrings", "rings"
    );

    /**
     * Noise signals — posts containing these are almost always off-topic.
     * Checking for these prevents wasting AI tokens on irrelevant content.
     */
    private static final List<String> NOISE_TERMS = List.of(
            "politics", "election", "government", "stock", "crypto", "bitcoin",
            "job", "career", "internship", "salary", "neet", "jee", "upsc",
            "relationship", "breakup", "marriage", "visa", "passport"
    );

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

        log.info("[REDDIT] Starting collection from {} subreddits (new + hot feeds)",
                subreddits.size());

        int totalCollected = 0;

        for (String sub : subreddits) {
            try {
                // Collect from both feeds — "new" gives recency, "hot" gives engagement signal
                int fromNew = collectFromSubreddit(sub, "new");
                Thread.sleep(1500);
                int fromHot = collectFromSubreddit(sub, "hot");

                int subTotal = fromNew + fromHot;
                totalCollected += subTotal;

                if (subTotal > 0) {
                    log.info("[REDDIT] r/{} → {} new signals (new={}, hot={})",
                            sub, subTotal, fromNew, fromHot);
                }

                Thread.sleep(1500); // polite delay between subreddits

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
    // COLLECT FROM ONE SUBREDDIT FEED
    // ─────────────────────────────────────────────────────────────

    private int collectFromSubreddit(String subreddit, String feed) {
        String url = "https://www.reddit.com/r/" + subreddit + "/" + feed + ".json?limit=50";
        log.debug("[REDDIT] Fetching r/{}/{}", subreddit, feed);

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(url)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("[REDDIT] HTTP error for r/{}/{}: {}", subreddit, feed, e.getMessage());
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
                String sourceId = data.path("id").asText("").trim();

                if (sourceId.isBlank()) continue;
                if (signalRepository.existsBySourceId(sourceId)) continue;

                long upvotes = data.path("score").asLong(0);
                if (upvotes < MIN_UPVOTES) continue;

                String title    = data.path("title").asText("").trim();
                String selftext = data.path("selftext").asText("").trim();

                if (title.isBlank()) continue;

                // Skip deleted or removed posts
                if ("[deleted]".equals(selftext) || "[removed]".equals(selftext)) {
                    selftext = "";
                }

                String combined = (title + " " + selftext).trim();

                // Drop obvious noise before hitting AI budget
                if (isNoise(combined)) {
                    log.debug("[REDDIT] Noise signal skipped: '{}'",
                            title.substring(0, Math.min(title.length(), 60)));
                    continue;
                }

                // Truncate to prevent DB bloat
                String content = combined.length() > MAX_CONTENT_LEN
                        ? combined.substring(0, MAX_CONTENT_LEN)
                        : combined;

                List<String> buyKeywords    = extractMatchingKeywords(content, BUY_INTENT_KEYWORDS);
                boolean hasProductKeyword   = hasAnyKeyword(content, PRODUCT_KEYWORDS);

                // Priority scoring:
                //   3 = has both buy intent AND product keyword (highest value)
                //   2 = has either buy intent or product keyword
                //   1 = baseline (still worth analyzing)
                int priorityScore;
                if (!buyKeywords.isEmpty() && hasProductKeyword) {
                    priorityScore = 3;
                } else if (!buyKeywords.isEmpty() || hasProductKeyword) {
                    priorityScore = 2;
                } else {
                    priorityScore = 1;
                }

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

                if (priorityScore == 3) {
                    log.debug("[REDDIT] High-priority signal: '{}' | keywords={}",
                            title.substring(0, Math.min(title.length(), 60)), buyKeywords);
                }

            } catch (Exception e) {
                log.warn("[REDDIT] Failed to process post in r/{}: {}", subreddit, e.getMessage());
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

    /**
     * Returns true if the content is dominated by off-topic noise terms.
     * A post is considered noise only if it has ZERO product/buy keywords
     * AND contains at least one noise term — to avoid false positives.
     */
    private boolean isNoise(String content) {
        if (content == null || content.isBlank()) return true;
        String lower = content.toLowerCase();
        boolean hasNoiseTerm = NOISE_TERMS.stream().anyMatch(lower::contains);
        if (!hasNoiseTerm) return false;
        // Even if noise term present, keep if a product keyword co-occurs
        boolean hasProductSignal = hasAnyKeyword(content, PRODUCT_KEYWORDS)
                || hasAnyKeyword(content, BUY_INTENT_KEYWORDS);
        return !hasProductSignal;
    }
}