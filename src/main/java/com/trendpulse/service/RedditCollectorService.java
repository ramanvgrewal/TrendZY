package com.trendpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trendpulse.entity.RawSignal;
import com.trendpulse.repository.RawSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedditCollectorService {

    private final RawSignalRepository rawSignalRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${reddit.user-agent}")
    private String userAgent;

    @Value("#{'${reddit.subreddits}'.split(',')}")
    private List<String> subreddits;

    @Value("${reddit.posts-per-subreddit}")
    private int postsPerSubreddit;

    // Buy-intent keywords
    private static final List<String> BUY_INTENT_KEYWORDS = Arrays.asList(
            "where to buy", "where can i buy", "where can i get",
            "link?", "link please", "drop the link", "drop link",
            "source?", "source please",
            "w2c", "wtc", "w 2 c",
            "id on this", "id on these",
            "take my money", "shut up and take my money",
            "cop or drop", "instant cop",
            "price?", "how much", "cost?",
            "anyone know where", "where did you get",
            "what brand", "which brand",
            "buying this", "just ordered", "just bought",
            "add to cart", "in my cart",
            "recommend", "recommendation", "recommendations",
            "suggest", "suggestion", "suggestions",
            "alternative", "alternatives",
            "best", "favorite", "favourite",
            "worth it", "worth buying", "worth the money",
            "must have", "must-have", "essential",
            "obsessed", "love this", "game changer", "game-changer",
            "review", "honest review",
            "haul", "shopping haul",
            "dupe", "dupes", "affordable",
            "looking for", "searching for", "help me find",
            "anyone tried", "has anyone",
            "what are your", "what do you",
            "top picks", "go-to", "go to",
            "underrated", "hidden gem", "slept on",
            "hyped", "overhyped", "overrated",
            "restock", "back in stock", "sold out",
            "wishlist", "wish list");

    private static final Pattern PRODUCT_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9]+(\\s+[A-Z][a-zA-Z0-9]+){0,3})\\b");

    private static final Pattern URL_ONLY_PATTERN = Pattern.compile("^\\s*(https?://\\S+)\\s*$");

    // ── Scheduled Collection ──

    @Scheduled(fixedRateString = "${reddit.collect-interval-ms}")
    public void collectRedditSignals() {
        log.info("🔍 Starting Reddit signal collection across {} subreddits", subreddits.size());
        int totalCollected = 0;

        for (String subreddit : subreddits) {
            try {
                int count = collectFromSubreddit(subreddit);
                totalCollected += count;
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("  ❌ Failed to collect from r/{}: {}", subreddit, e.getMessage());
            }
        }

        log.info("📊 Collection complete: {} total signals collected", totalCollected);
    }

    public int manualCollect() {
        log.info("🔍 Manual collection triggered");
        int totalCollected = 0;
        for (String subreddit : subreddits) {
            try {
                totalCollected += collectFromSubreddit(subreddit);
                Thread.sleep(4000);
            } catch (Exception e) {
                log.error("Failed to collect from r/{}: {}", subreddit, e.getMessage());
            }
        }
        return totalCollected;
    }

    private int collectFromSubreddit(String subreddit) {
        WebClient client = webClientBuilder.build();
        int accepted = 0;
        int rejected = 0;

        try {
            // Fetch new posts
            JsonNode postsResponse = client.get()
                    .uri("https://www.reddit.com/r/{subreddit}/new.json?limit={limit}",
                            subreddit, postsPerSubreddit)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (postsResponse != null && postsResponse.has("data")) {
                JsonNode children = postsResponse.get("data").get("children");
                if (children != null && children.isArray()) {
                    for (JsonNode child : children) {
                        JsonNode postData = child.get("data");
                        int result = processPost(subreddit, postData);
                        if (result > 0)
                            accepted++;
                        else if (result < 0)
                            rejected++;
                    }
                }
            }

            Thread.sleep(3000);

            // Fetch hot posts for comments
            JsonNode hotResponse = client.get()
                    .uri("https://www.reddit.com/r/{subreddit}/hot.json?limit={limit}",
                            subreddit, 5)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (hotResponse != null && hotResponse.has("data")) {
                JsonNode hotChildren = hotResponse.get("data").get("children");
                if (hotChildren != null && hotChildren.isArray()) {
                    for (JsonNode child : hotChildren) {
                        JsonNode postData = child.get("data");
                        String postId = postData.has("id") ? postData.get("id").asText() : "";
                        int[] commentResults = collectCommentsForPost(subreddit, postId, client);
                        accepted += commentResults[0];
                        rejected += commentResults[1];
                        Thread.sleep(3000);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error fetching from r/{}: {}", subreddit, e.getMessage());
        }

        log.info("  📊 r/{}: {} accepted, {} rejected", subreddit, accepted, rejected);
        return accepted;
    }

    // ── Quality Gate ──

    private String checkSignalQuality(JsonNode data, boolean isComment) {
        // Check author for bots
        String author = data.has("author") ? data.get("author").asText() : "";
        if (author.toLowerCase().contains("bot") || author.toLowerCase().contains("automoderator")) {
            return "bot/automoderator author: " + author;
        }

        if (isComment) {
            // Comment-specific quality checks
            String body = data.has("body") ? data.get("body").asText() : "";
            int score = data.has("score") ? data.get("score").asInt() : 0;

            if (score < 2) {
                return "comment score too low: " + score;
            }
            if (body.trim().length() < 15) {
                return "comment too short: " + body.trim().length() + " chars";
            }
            if (URL_ONLY_PATTERN.matcher(body.trim()).matches()) {
                return "comment is just a URL";
            }
        } else {
            // Post-specific quality checks
            int ups = data.has("ups") ? data.get("ups").asInt() : 0;
            if (ups < 10) {
                return "post upvotes too low: " + ups;
            }
        }

        return null; // passes quality gate
    }

    // ── Post Processing ──

    private int processPost(String subreddit, JsonNode postData) {
        if (postData == null)
            return 0;

        String postId = postData.has("id") ? postData.get("id").asText() : "";
        String title = postData.has("title") ? postData.get("title").asText() : "";
        String selftext = postData.has("selftext") ? postData.get("selftext").asText() : "";
        String author = postData.has("author") ? postData.get("author").asText() : "";

        String combinedText = (title + " " + selftext).toLowerCase();
        String matchedKeyword = findMatchingKeyword(combinedText);
        if (matchedKeyword == null)
            return 0;

        if (rawSignalRepository.existsByPostIdAndSubreddit(postId, subreddit))
            return 0;

        // Quality gate
        String rejectReason = checkSignalQuality(postData, false);
        if (rejectReason != null) {
            log.debug("  ⚠️ Skipped low-quality signal: {}", rejectReason);
            return -1; // rejected
        }

        String productMention = extractProductMention(title + " " + selftext);

        RawSignal signal = RawSignal.builder()
                .source("reddit")
                .subreddit(subreddit)
                .postId(postId)
                .postTitle(title)
                .commentBody(selftext.length() > 500 ? selftext.substring(0, 500) : selftext)
                .author(author)
                .keywordMatched(matchedKeyword)
                .productMentioned(productMention)
                .collectedAt(LocalDateTime.now())
                .processed(false)
                .build();

        rawSignalRepository.save(signal);
        log.debug("  ✅ Quality signal saved: [r/{}] {}", subreddit,
                title.length() > 60 ? title.substring(0, 60) + "..." : title);
        return 1; // accepted
    }

    // ── Comment Processing ──

    private int[] collectCommentsForPost(String subreddit, String postId, WebClient client) {
        int accepted = 0;
        int rejected = 0;

        try {
            JsonNode commentsResponse = client.get()
                    .uri("https://www.reddit.com/r/{subreddit}/comments/{postId}.json?limit=50",
                            subreddit, postId)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (commentsResponse != null && commentsResponse.isArray() && commentsResponse.size() > 1) {
                JsonNode commentData = commentsResponse.get(1).get("data").get("children");
                String postTitle = "";
                if (commentsResponse.get(0).has("data")) {
                    JsonNode postChildren = commentsResponse.get(0).get("data").get("children");
                    if (postChildren != null && postChildren.isArray() && postChildren.size() > 0) {
                        postTitle = postChildren.get(0).get("data").has("title")
                                ? postChildren.get(0).get("data").get("title").asText()
                                : "";
                    }
                }

                if (commentData != null && commentData.isArray()) {
                    for (JsonNode comment : commentData) {
                        if (!comment.has("data"))
                            continue;
                        JsonNode cData = comment.get("data");
                        String body = cData.has("body") ? cData.get("body").asText() : "";
                        String author = cData.has("author") ? cData.get("author").asText() : "";
                        String commentId = cData.has("id") ? cData.get("id").asText() : "";

                        String matchedKeyword = findMatchingKeyword(body.toLowerCase());
                        if (matchedKeyword == null)
                            continue;
                        if (rawSignalRepository.existsByPostIdAndSubreddit(commentId, subreddit))
                            continue;

                        // Quality gate for comment
                        String rejectReason = checkSignalQuality(cData, true);
                        if (rejectReason != null) {
                            log.debug("  ⚠️ Skipped low-quality signal: {}", rejectReason);
                            rejected++;
                            continue;
                        }

                        String productMention = extractProductMention(postTitle + " " + body);

                        RawSignal signal = RawSignal.builder()
                                .source("reddit")
                                .subreddit(subreddit)
                                .postId(commentId)
                                .postTitle(postTitle)
                                .commentBody(body.length() > 500 ? body.substring(0, 500) : body)
                                .author(author)
                                .keywordMatched(matchedKeyword)
                                .productMentioned(productMention)
                                .collectedAt(LocalDateTime.now())
                                .processed(false)
                                .build();

                        rawSignalRepository.save(signal);
                        log.debug("  ✅ Quality signal saved: comment by {}", author);
                        accepted++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch comments for post {}: {}", postId, e.getMessage());
        }

        return new int[] { accepted, rejected };
    }

    private String findMatchingKeyword(String text) {
        for (String keyword : BUY_INTENT_KEYWORDS) {
            if (text.contains(keyword))
                return keyword;
        }
        return null;
    }

    private String extractProductMention(String text) {
        if (text == null || text.isBlank())
            return null;

        Matcher matcher = PRODUCT_PATTERN.matcher(text);
        if (matcher.find())
            return matcher.group(1).trim();

        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 3 && Character.isUpperCase(word.charAt(0))
                    && !List.of("The", "This", "That", "What", "Where", "When", "How", "Does",
                            "Can", "Could", "Would", "Should", "Just", "Really", "Very").contains(word)) {
                return word;
            }
        }
        return null;
    }
}
