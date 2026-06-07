package com.trendzy.ingestion.scraper.instagram;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.trendzy.ingestion.model.Platform;
import com.trendzy.ingestion.model.TrendSignal;
import com.trendzy.ingestion.scraper.util.RandomDelayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes Instagram's Explore / hashtag pages for trend signals.
 *
 * <h3>V1 (legacy) — {@link #fetchExplorePosts(Playwright, String)}</h3>
 * <p>Returns a list of post/reel URLs only. Used by the old product-discovery pipeline
 * ({@code ScraperOrchestratorService}). <strong>Kept intact — do not modify.</strong></p>
 *
 * <h3>V2 — {@link #fetchExploreSignals(Playwright, String)}</h3>
 * <p>Returns fully-populated {@link TrendSignal} objects with caption text, hashtags,
 * engagement scores, author username, and media URL. This is the new entry point for
 * the TrendZY V2 ingestion pipeline.</p>
 *
 * <h4>V2 Optimisation: Network Interception</h4>
 * <p>When navigating to individual post pages (to extract metadata that is not available
 * in the tag-page grid), the Playwright context intercepts and <strong>aborts</strong>
 * requests for images, fonts, stylesheets, and media files. This reduces page-load time
 * by ~60-70% since we only need the DOM text and embedded JSON payloads.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstagramExploreClient {

    private final InstagramSessionManager sessionManager;

    // ─────────────────────────────────────────────────────────────
    // CONSTANTS
    // ─────────────────────────────────────────────────────────────

    private static final String EXPLORE_URL        = "https://www.instagram.com/explore/";
    private static final String SEARCH_URL_TMPL    = "https://www.instagram.com/explore/tags/%s/";
    private static final String USER_AGENT         =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int    SCROLL_COUNT       = 6;
    private static final int    MAX_POSTS          = 30;
    private static final int    DOM_SETTLE_TIMEOUT = 10_000;
    private static final int    POST_PAGE_TIMEOUT  = 8_000;

    /** Hashtag searches in specific order as per Phase 1. */
    private static final Map<String, List<String>> SECTION_TAGS = Map.of(
            "STREETWEAR", List.of("streetwearindia", "indianstreetwear", "d2cbrand", "oversizedtshirt", "y2kfashionindia", "genzfashion", "indiad2c", "newdrop", "brandedclothing"),
            "CRICKET", List.of("cricketindia", "indiancricket", "cricketmerch", "bleedblue", "cricketfans", "iplmerch", "cricketapparel", "cricketjersey"),
            "GYM", List.of("gymwearindia", "activewearindia", "fitnessapparel", "gymgear", "workoutclothes", "gymfashion", "athleisureindia", "indianfitness"),
            "ANIME", List.of("animeclothingindia", "animemerchindia", "otakufashion", "animeapparel", "animestreetwear", "weebmerch", "animeindia", "mangaapparel"),
            "SNEAKERS", List.of("sneakerheadindia", "sneakersindia", "kicksindia", "sneakerstore", "solecollectorindia", "streetwearsneakers", "sneakercommunityindia"),
            "CODING", List.of("devmerch", "codingtshirts", "programmerclothing", "techapparel", "developersindia", "softwareengineer", "codinglife", "techmerch")
    );
    private static final int SCROLL_COUNT_PER_TAG = 2;
    private static final int POSTS_PER_TAG        = 8;
    private static final int MIN_POSTS_TOTAL      = 30;

    private static final Pattern POST_OR_REEL_PATTERN =
            Pattern.compile("https://www\\.instagram\\.com/(p|reel)/[A-Za-z0-9_-]+/?");

    // ── Patterns for extracting post metadata from Instagram's embedded JSON ──

    /** Matches the caption / "text" field in Instagram's embedded JSON. */
    private static final Pattern CAPTION_JSON_PATTERN = Pattern.compile(
            "\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Matches an og:description meta tag (fallback for caption). */
    private static final Pattern OG_DESC_PATTERN = Pattern.compile(
            "<meta\\s+(?:property|name)\\s*=\\s*\"og:description\"\\s+content\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
            Pattern.CASE_INSENSITIVE);

    /** Matches like_count or edge_media_preview_like count in embedded JSON. */
    private static final Pattern LIKE_COUNT_PATTERN = Pattern.compile(
            "\"(?:like_count|edge_media_preview_like)\"\\s*[:{]\\s*(?:\"count\"\\s*:\\s*)?(\\d+)");

    /** Matches the owner username in embedded JSON. */
    private static final Pattern OWNER_USERNAME_PATTERN = Pattern.compile(
            "\"owner\"\\s*:\\s*\\{[^}]*\"username\"\\s*:\\s*\"([A-Za-z0-9._]+)\"");

    /** Matches og:image meta tag for the media URL. */
    private static final Pattern OG_IMAGE_PATTERN = Pattern.compile(
            "<meta\\s+(?:property|name)\\s*=\\s*\"og:image\"\\s+content\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
            Pattern.CASE_INSENSITIVE);

    /** Extracts hashtags from caption text. */
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");

    /** Resource types to block when scraping individual post pages. */
    private static final Set<String> BLOCKED_RESOURCE_TYPES = Set.of(
            "image", "media", "font", "stylesheet"
    );

    /** Reserved Instagram paths — not usernames. */
    private static final List<String> RESERVED_PATHS = List.of(
            "explore", "p", "reel", "stories", "accounts", "tags", "direct"
    );

    // ═════════════════════════════════════════════════════════════
    //  V1 — LEGACY: fetchExplorePosts (returns URLs only)
    //  ⚠ DO NOT MODIFY — used by ScraperOrchestratorService
    // ═════════════════════════════════════════════════════════════

    /**
     * Returns a de-duplicated list of Instagram post URLs found on specific hashtag pages.
     * Follows Phase 1 instructions.
     *
     * @param playwright a live {@link Playwright} instance owned by the caller
     * @param section the specific section corresponding to hashtags to scrape
     */
    public List<String> fetchExplorePosts(Playwright playwright, String section) {
        Set<String> postUrls = new LinkedHashSet<>();

        if (!sessionManager.ensureSession(playwright)) {
            log.warn("[EXPLORE] Cannot proceed — no valid session");
            return List.of();
        }

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = createContext(browser);
            Page page = context.newPage();

            // ── Phase 1: Hashtag Discovery ─────────────────
            List<String> currentTags = SECTION_TAGS.getOrDefault(section == null ? "" : section.toUpperCase(), SECTION_TAGS.get("STREETWEAR"));
            for (String tag : currentTags) {
                if (postUrls.size() >= MIN_POSTS_TOTAL) break;
                String tagUrl = String.format(SEARCH_URL_TMPL, tag);
                collectFromTagUrl(page, tagUrl, postUrls);
                RandomDelayUtil.delay();
            }

            context.close();

        } catch (Exception e) {
            log.error("[EXPLORE] Fatal error: {}", e.getMessage(), e);
        }

        List<String> result = new ArrayList<>(postUrls);
        log.info("[EXPLORE] Returning {} post URLs", result.size());
        return result;
    }

    // ═════════════════════════════════════════════════════════════
    //  V2 — NEW: fetchExploreSignals (returns TrendSignal objects)
    // ═════════════════════════════════════════════════════════════

    /**
     * Scrapes Instagram hashtag pages and then navigates into each individual post
     * to extract rich signal data: caption, hashtags, engagement score, author, and media URL.
     *
     * <p><strong>Performance optimisation:</strong> Network interception aborts all image,
     * font, stylesheet, and media requests when loading individual post pages, since
     * we only need DOM text and embedded JSON payloads.</p>
     *
     * @param playwright a live {@link Playwright} instance owned by the caller
     * @param section    the section/category determining which hashtags to scrape
     * @return list of {@link TrendSignal} objects with {@code platform = INSTAGRAM}
     */
    public List<TrendSignal> fetchExploreSignals(Playwright playwright, String section) {
        log.info("[EXPLORE-V2] ════════ Starting signal extraction for section: {} ════════", section);

        List<TrendSignal> signals = new ArrayList<>();

        if (!sessionManager.ensureSession(playwright)) {
            log.warn("[EXPLORE-V2] Cannot proceed — no valid Instagram session");
            return signals;
        }

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        Set<String> postUrls = null;
        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = createContext(browser);

            // ── Phase 1: Collect post URLs from hashtag pages ──────────
            // (reuses existing tag-page scrolling logic; no resource blocking here
            //  because the tag page grid needs images to fully render the DOM)
            postUrls = new LinkedHashSet<>();
            Page tagPage = context.newPage();

            List<String> currentTags = SECTION_TAGS.getOrDefault(
                    section == null ? "" : section.toUpperCase(),
                    SECTION_TAGS.get("STREETWEAR"));

            for (String tag : currentTags) {
                if (postUrls.size() >= MIN_POSTS_TOTAL) break;
                String tagUrl = String.format(SEARCH_URL_TMPL, tag);
                collectFromTagUrl(tagPage, tagUrl, postUrls);
                RandomDelayUtil.delay();
            }

            tagPage.close();
            log.info("[EXPLORE-V2] Phase 1 complete — collected {} post URLs from tag pages", postUrls.size());

            if (postUrls.isEmpty()) {
                log.warn("[EXPLORE-V2] No post URLs found — returning empty signal list");
                context.close();
                return signals;
            }

            // ── Phase 2: Navigate into each post to extract signal data ──
            // Open a NEW page with resource interception enabled for speed
            Page signalPage = context.newPage();
            installResourceBlocker(signalPage);

            int processed = 0;
            int totalPosts = postUrls.size();

            for (String postUrl : postUrls) {
                processed++;
                log.info("[EXPLORE-V2] Extracting signal {}/{}: {}", processed, totalPosts, postUrl);

                try {
                    TrendSignal signal = extractSignalFromPost(signalPage, postUrl);
                    if (signal != null) {
                        signals.add(signal);
                        log.info("[EXPLORE-V2] ✅ Signal extracted — @{} | likes={} | hashtags={} | caption={}",
                                signal.getAuthorUsername(),
                                signal.getEngagementScore(),
                                signal.getHashtags().size(),
                                truncate(signal.getRawText(), 80));
                    } else {
                        log.debug("[EXPLORE-V2] ⚠ Could not extract signal from: {}", postUrl);
                    }
                } catch (TimeoutError te) {
                    log.warn("[EXPLORE-V2] Timeout loading post page — skipping: {}", postUrl);
                } catch (Exception e) {
                    log.warn("[EXPLORE-V2] Error extracting signal from {}: {}", postUrl, e.getMessage());
                }

                // Anti-detection delay between post navigations
                RandomDelayUtil.shortDelay();

                // Extended pause every 10 posts to avoid rate limiting
                if (processed % 10 == 0 && processed < totalPosts) {
                    log.info("[EXPLORE-V2] Processed {} posts — pausing to avoid rate limiting", processed);
                    RandomDelayUtil.delay(8_000, 12_000, "v2-rate-limit-pause");
                }
            }

            signalPage.close();
            context.close();

        } catch (Exception e) {
            log.error("[EXPLORE-V2] Fatal error during signal extraction: {}", e.getMessage(), e);
        }

        assert postUrls != null;
        log.info("[EXPLORE-V2] ════════ Signal extraction complete: {} signals from {} posts ════════",
                signals.size(), postUrls.size());
        return signals;
    }

    // ─────────────────────────────────────────────────────────────
    //  V2 INTERNALS — Signal Extraction from Individual Post Page
    // ─────────────────────────────────────────────────────────────

    /**
     * Navigates to an individual Instagram post page and extracts all available
     * signal data from the DOM and embedded JSON.
     *
     * <p>Extraction priority for each field:
     * <ul>
     *   <li><strong>Caption:</strong> embedded JSON {@code "text":"..."} → {@code og:description} meta tag</li>
     *   <li><strong>Likes:</strong> JSON {@code "like_count":N} or {@code "edge_media_preview_like":{"count":N}}</li>
     *   <li><strong>Author:</strong> JSON {@code "owner":{"username":"..."}}</li>
     *   <li><strong>Media URL:</strong> {@code og:image} meta tag</li>
     *   <li><strong>Hashtags:</strong> parsed from caption text via {@code #(\w+)} regex</li>
     * </ul>
     *
     * @param page    a Playwright page with resource blocking already installed
     * @param postUrl the full Instagram post or reel URL
     * @return a populated {@link TrendSignal}, or {@code null} if extraction failed completely
     */
    private TrendSignal extractSignalFromPost(Page page, String postUrl) {
        page.navigate(postUrl, new Page.NavigateOptions().setTimeout(POST_PAGE_TIMEOUT));

        try {
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(POST_PAGE_TIMEOUT));
        } catch (TimeoutError te) {
            log.debug("[EXPLORE-V2] DOM content timeout for post — continuing with partial data: {}", postUrl);
        }

        // Check for session expiry (redirect to login page)
        if (isLoginPage(page.url())) {
            log.warn("[EXPLORE-V2] Session expired — redirected to login page");
            sessionManager.invalidateSession();
            return null;
        }

        // Small settle delay for any remaining JS to hydrate the embedded JSON
        RandomDelayUtil.delay(500, 1000, "post-settle");

        // Get the full page HTML once — all extraction happens against this string
        String html;
        try {
            html = page.content();
        } catch (Exception e) {
            log.warn("[EXPLORE-V2] Failed to get page content for {}: {}", postUrl, e.getMessage());
            return null;
        }

        if (html == null || html.length() < 200) {
            log.debug("[EXPLORE-V2] Page content too small — likely blocked: {}", postUrl);
            return null;
        }

        // ── Extract caption ──────────────────────────────────
        String caption = extractCaption(html);

        // ── Extract hashtags from caption ─────────────────────
        List<String> hashtags = extractHashtags(caption);

        // ── Extract engagement score (like count) ─────────────
        long likeCount = extractLikeCount(html);

        // ── Extract author username ───────────────────────────
        String authorUsername = extractAuthorUsername(html);

        // ── Extract media URL ─────────────────────────────────
        String mediaUrl = extractMediaUrl(html);

        // Validate: we need at least a caption OR a like count to call this a useful signal
        if ((caption == null || caption.isBlank()) && likeCount == 0) {
            log.debug("[EXPLORE-V2] No caption and no engagement — skipping: {}", postUrl);
            return null;
        }

        return TrendSignal.builder()
                .platform(Platform.INSTAGRAM)
                .sourceUrl(postUrl)
                .rawText(caption != null ? caption : "")
                .hashtags(hashtags)
                .engagementScore(likeCount)
                .authorUsername(authorUsername != null ? authorUsername : "unknown")
                .mediaUrl(mediaUrl)
                .collectedAt(Instant.now())
                .processedByAi(false)
                .build();
    }

    /**
     * Extracts the post caption from Instagram's embedded JSON or og:description meta tag.
     *
     * <p><strong>Strategy:</strong> Instagram embeds post data as JSON in {@code <script>} tags.
     * The caption is found in {@code "text":"..."} fields. We prefer the longest match
     * (since the page may contain multiple "text" fields for comments, etc.)
     * and fall back to the {@code og:description} meta tag.</p>
     */
    private String extractCaption(String html) {
        // Strategy 1: Find the longest "text":"..." value in embedded JSON
        // This is typically the main caption (comments are shorter)
        String bestCaption = null;
        Matcher m = CAPTION_JSON_PATTERN.matcher(html);
        while (m.find()) {
            String candidate = unescapeJsonString(m.group(1));
            // Filter out very short strings (likely not the caption) and Instagram UI text
            if (candidate != null && candidate.length() > 5
                    && !candidate.startsWith("Follow")
                    && !candidate.startsWith("Log in")
                    && !candidate.contains("Instagram")) {
                if (bestCaption == null || candidate.length() > bestCaption.length()) {
                    bestCaption = candidate;
                }
            }
        }

        if (bestCaption != null && bestCaption.length() > 10) {
            log.debug("[EXPLORE-V2] Caption via JSON 'text' field ({} chars)", bestCaption.length());
            return bestCaption.trim();
        }

        // Strategy 2: og:description meta tag
        Matcher ogMatcher = OG_DESC_PATTERN.matcher(html);
        if (ogMatcher.find()) {
            String ogDesc = unescapeHtmlEntities(ogMatcher.group(1));
            if (ogDesc != null && ogDesc.length() > 10) {
                log.debug("[EXPLORE-V2] Caption via og:description ({} chars)", ogDesc.length());
                return ogDesc.trim();
            }
        }

        // Strategy 3: Try to extract from accessibility text on images
        // Instagram sometimes has: alt="Photo by @user on ... Caption text here"
        Matcher altMatcher = Pattern.compile("alt=\"Photo by[^\"]*?\\. ([^\"]{20,})\"").matcher(html);
        if (altMatcher.find()) {
            String altCaption = altMatcher.group(1);
            log.debug("[EXPLORE-V2] Caption via img alt text ({} chars)", altCaption.length());
            return altCaption.trim();
        }

        log.debug("[EXPLORE-V2] No caption found in page HTML");
        return null;
    }

    /**
     * Extracts hashtags from the caption text.
     * Returns tags WITHOUT the leading '#' symbol (e.g. "streetwear", not "#streetwear").
     */
    private List<String> extractHashtags(String caption) {
        List<String> hashtags = new ArrayList<>();
        if (caption == null || caption.isBlank()) return hashtags;

        Matcher m = HASHTAG_PATTERN.matcher(caption);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            if (tag.length() >= 2 && tag.length() <= 50 && seen.add(tag)) {
                hashtags.add(tag);
            }
        }

        log.debug("[EXPLORE-V2] Extracted {} unique hashtags from caption", hashtags.size());
        return hashtags;
    }

    /**
     * Extracts the like count from Instagram's embedded JSON.
     *
     * <p>Looks for either of:
     * <ul>
     *   <li>{@code "like_count": 12345} — newer Instagram API format</li>
     *   <li>{@code "edge_media_preview_like": {"count": 12345}} — older GraphQL format</li>
     * </ul>
     */
    private long extractLikeCount(String html) {
        // Try all matches and return the largest (most likely the post's own like count,
        // not a comment's like count)
        long maxLikes = 0;
        Matcher m = LIKE_COUNT_PATTERN.matcher(html);
        while (m.find()) {
            try {
                long count = Long.parseLong(m.group(1));
                if (count > maxLikes) {
                    maxLikes = count;
                }
            } catch (NumberFormatException ignored) {
                // Silently skip malformed numbers
            }
        }

        if (maxLikes > 0) {
            log.debug("[EXPLORE-V2] Like count: {}", maxLikes);
        } else {
            log.debug("[EXPLORE-V2] Could not extract like count from page HTML");
        }

        return maxLikes;
    }

    /**
     * Extracts the post author's username from the embedded JSON {@code "owner"} block.
     */
    private String extractAuthorUsername(String html) {
        Matcher m = OWNER_USERNAME_PATTERN.matcher(html);
        if (m.find()) {
            String username = m.group(1);
            if (!RESERVED_PATHS.contains(username.toLowerCase()) && username.length() > 1) {
                log.debug("[EXPLORE-V2] Author username: @{}", username);
                return username;
            }
        }

        // Fallback: try generic "username":"..." pattern
        Matcher fallback = Pattern.compile("\"username\"\\s*:\\s*\"([A-Za-z0-9._]+)\"").matcher(html);
        while (fallback.find()) {
            String u = fallback.group(1);
            if (!RESERVED_PATHS.contains(u.toLowerCase()) && u.length() > 2) {
                log.debug("[EXPLORE-V2] Author username (fallback): @{}", u);
                return u;
            }
        }

        log.debug("[EXPLORE-V2] Could not extract author username");
        return null;
    }

    /**
     * Extracts the primary media URL from the {@code og:image} meta tag.
     */
    private String extractMediaUrl(String html) {
        Matcher m = OG_IMAGE_PATTERN.matcher(html);
        if (m.find()) {
            String url = unescapeHtmlEntities(m.group(1));
            if (url != null && url.startsWith("http")) {
                log.debug("[EXPLORE-V2] Media URL extracted (og:image)");
                return url;
            }
        }

        // Fallback: look for display_url in JSON
        Matcher displayUrl = Pattern.compile("\"display_url\"\\s*:\\s*\"(https?://[^\"]+)\"").matcher(html);
        if (displayUrl.find()) {
            String url = displayUrl.group(1).replace("\\/", "/");
            log.debug("[EXPLORE-V2] Media URL extracted (display_url JSON)");
            return url;
        }

        log.debug("[EXPLORE-V2] Could not extract media URL");
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  NETWORK INTERCEPTION — Block unnecessary assets for speed
    // ─────────────────────────────────────────────────────────────

    /**
     * Installs a route handler on the given page that aborts requests for images,
     * fonts, stylesheets, and media files. This drastically reduces page load time
     * when we only need DOM text and JSON payloads.
     *
     * <p>Requests that <em>are</em> allowed through:
     * <ul>
     *   <li>HTML documents (the page itself)</li>
     *   <li>XHR / Fetch requests (may contain API data)</li>
     *   <li>Scripts (needed for JSON hydration)</li>
     * </ul>
     */
    private void installResourceBlocker(Page page) {
        page.route("**/*", route -> {
            String resourceType = route.request().resourceType();
            if (BLOCKED_RESOURCE_TYPES.contains(resourceType)) {
                route.abort();
            } else {
                route.resume();
            }
        });
        log.debug("[EXPLORE-V2] Resource blocker installed — blocking: {}", BLOCKED_RESOURCE_TYPES);
    }

    // ─────────────────────────────────────────────────────────────
    //  SHARED PRIVATE METHODS (used by both V1 and V2)
    // ─────────────────────────────────────────────────────────────

    /**
     * Navigates to a hashtag tag page, scrolls to load more posts, and collects
     * post/reel URLs into the provided accumulator set.
     *
     * <p>This method is shared between V1 ({@link #fetchExplorePosts}) and
     * V2 ({@link #fetchExploreSignals}) — the tag-page URL collection logic
     * is identical for both paths.</p>
     */
    private void collectFromTagUrl(Page page, String url, Set<String> accumulator) {
        try {
            log.info("[EXPLORE] Navigating → {}", url);
            page.navigate(url);
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(DOM_SETTLE_TIMEOUT));

            // Detect session expiry
            if (isLoginPage(page.url())) {
                log.warn("[EXPLORE] Redirected to login — session expired");
                sessionManager.invalidateSession();
                return;
            }

            RandomDelayUtil.longDelay();

            // Scroll 2-3 times to load more posts
            for (int i = 0; i < SCROLL_COUNT_PER_TAG; i++) {
                page.evaluate("window.scrollBy(0, window.innerHeight * 1.5)");
                RandomDelayUtil.delay();
                log.debug("[EXPLORE] Scroll {}/{} on {}", i + 1, SCROLL_COUNT_PER_TAG, url);
            }

            // Extract post/reel links from <a> tags, limit to POSTS_PER_TAG from this hashtag
            List<ElementHandle> anchors = page.querySelectorAll("a[href*='/p/'], a[href*='/reel/']");
            log.debug("[EXPLORE] Found {} anchor elements with /p/ or /reel/ links on {}", anchors.size(), url);

            int collectedThisTag = 0;
            for (ElementHandle a : anchors) {
                if (collectedThisTag >= POSTS_PER_TAG) break;
                try {
                    String href = a.getAttribute("href");
                    if (href == null || href.isBlank()) continue;
                    String full = href.startsWith("http")
                            ? href
                            : "https://www.instagram.com" + href;
                    // Normalise trailing slash
                    if (!full.endsWith("/")) full += "/";
                    // Validate format
                    String cleanUrl = full.split("\\?")[0];
                    if (POST_OR_REEL_PATTERN.matcher(cleanUrl).matches()) {
                        if (accumulator.add(cleanUrl)) {
                            collectedThisTag++;
                        }
                    }
                } catch (Exception e) {
                    log.trace("[EXPLORE] Skipping anchor: {}", e.getMessage());
                }
            }
            log.info("[EXPLORE] Collected {} unique posts from {}. Total unique: {}", collectedThisTag, url, accumulator.size());

        } catch (Exception e) {
            log.warn("[EXPLORE] Failed to collect from {}: {}", url, e.getMessage());
        }
    }

    private BrowserContext createContext(Browser browser) {
        return browser.newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(sessionManager.getSessionPath())
                        .setViewportSize(1280, 900)
                        .setUserAgent(USER_AGENT));
    }

    private boolean isLoginPage(String url) {
        return url != null
                && (url.contains("/accounts/login") || url.contains("/accounts/emailsignup"));
    }

    // ─────────────────────────────────────────────────────────────
    //  STRING UTILITIES
    // ─────────────────────────────────────────────────────────────

    /**
     * Unescapes common JSON string escape sequences.
     */
    private static String unescapeJsonString(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    /**
     * Unescapes common HTML entities found in meta tag content attributes.
     */
    private static String unescapeHtmlEntities(String s) {
        if (s == null) return null;
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&apos;", "'");
    }

    /**
     * Truncates a string to the specified maximum length, appending "..." if truncated.
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "<null>";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
