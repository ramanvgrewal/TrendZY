package com.trendzy.scraper.instagram;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.trendzy.scraper.util.RandomDelayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scrapes Instagram's Explore page to find streetwear-related post URLs.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Load Explore with the stored session (headless).</li>
 *   <li>Scroll 5–8 times to surface more content.</li>
 *   <li>Collect all {@code /p/…} post links visible in the grid.</li>
 *   <li>Optionally search for streetwear-related terms when the explore grid
 *       does not yield enough posts.</li>
 * </ol>
 *
 * <p>Returns at most {@value #MAX_POSTS} URLs.  Duplicates are eliminated.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstagramExploreClient {

    private final InstagramSessionManager sessionManager;

    private static final String EXPLORE_URL        = "https://www.instagram.com/explore/";
    private static final String SEARCH_URL_TMPL    = "https://www.instagram.com/explore/tags/%s/";
    private static final String USER_AGENT         =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int    SCROLL_COUNT       = 6;
    private static final int    MAX_POSTS          = 30;
    private static final int    DOM_SETTLE_TIMEOUT = 10_000;

    /** Hashtag searches in specific order as per Phase 1. */
    private static final List<String> STREETWEAR_TAGS = List.of(
            "streetwearindia",
            "indianstreetwear",
            "d2cbrand",
            "oversizedtshirt",
            "y2kfashionindia",
            "genzfashion",
            "indiad2c",
            "newdrop",
            "brandedclothing"
    );
    private static final int    SCROLL_COUNT_PER_TAG = 2; // Scroll 2-3 times
    private static final int    POSTS_PER_TAG        = 8; // Collect 5-8 posts per hashtag
    private static final int    MIN_POSTS_TOTAL      = 30; // Target 30-40 unique post URLs

    private static final Pattern POST_OR_REEL_PATTERN =
            Pattern.compile("https://www\\.instagram\\.com/(p|reel)/[A-Za-z0-9_-]+/?");

    // ─────────────────────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a de-duplicated list of Instagram post URLs found on specific hashtag pages.
     * Follows Phase 1 instructions.
     *
     * @param playwright a live {@link Playwright} instance owned by the caller
     */
    public List<String> fetchExplorePosts(Playwright playwright) {
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
            for (String tag : STREETWEAR_TAGS) {
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

    // ─────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────

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
}
