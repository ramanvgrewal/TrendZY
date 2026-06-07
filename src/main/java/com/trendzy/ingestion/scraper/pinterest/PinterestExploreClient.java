package com.trendzy.ingestion.scraper.pinterest;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes Pinterest search results to discover Pins and extracts rich metadata
 * to populate {@link TrendSignal} objects.
 *
 * <p>Implements Playwright network interception to abort unnecessary resources
 * (images, media, stylesheets, fonts) when navigating into individual Pin pages
 * for deep extraction, optimizing performance and bandwidth.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PinterestExploreClient {

    private final PinterestSessionManager sessionManager;

    private static final String SEARCH_URL_TMPL = "https://www.pinterest.com/search/pins/?q=%s";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";

    private static final int DOM_SETTLE_TIMEOUT = 12_000;
    private static final int POST_PAGE_TIMEOUT  = 8_000;
    private static final int SCROLL_COUNT       = 5;

    // We block these resource types to drastically speed up navigation into individual pin pages
    private static final Set<String> BLOCKED_RESOURCE_TYPES = Set.of(
            "image", "media", "font", "stylesheet"
    );

    // Matches valid Pinterest Pin URLs (e.g. /pin/123456789/)
    private static final Pattern PIN_URL_PATTERN = Pattern.compile("https://www\\.pinterest\\.com/pin/\\d+/?");

    // Matches hashtags in descriptions
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([A-Za-z0-9_]+)");

    /**
     * Executes the Pinterest search and extracts TrendSignals.
     *
     * @param playwright  A live Playwright instance.
     * @param searchQuery The query to search for (e.g., "vintage streetwear").
     * @return A list of extracted TrendSignals.
     */
    public List<TrendSignal> fetchExploreSignals(Playwright playwright, String searchQuery) {
        log.info("[PINTEREST-EXPLORE] ════════ Starting search for query: '{}' ════════", searchQuery);

        List<TrendSignal> signals = new ArrayList<>();

        if (!sessionManager.ensureSession(playwright)) {
            log.warn("[PINTEREST-EXPLORE] Cannot proceed — no valid Pinterest session");
            return signals;
        }

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        Set<String> pinUrls = new java.util.LinkedHashSet<>();
        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = createContext(browser);

            // ── Phase 1: Search and Collect Pin URLs ────────────────
            pinUrls = collectPinUrlsFromSearch(context, searchQuery);

            if (pinUrls.isEmpty()) {
                log.warn("[PINTEREST-EXPLORE] No pins found for query: {}", searchQuery);
                context.close();
                return signals;
            }

            log.info("[PINTEREST-EXPLORE] Phase 1 complete. Collected {} unique pin URLs.", pinUrls.size());

            // ── Phase 2: Navigate into each Pin for deep extraction ──
            Page signalPage = context.newPage();
            installResourceBlocker(signalPage);

            int processed = 0;
            int totalPins = pinUrls.size();

            for (String pinUrl : pinUrls) {
                processed++;
                log.info("[PINTEREST-EXPLORE] Extracting signal {}/{}: {}", processed, totalPins, pinUrl);

                try {
                    TrendSignal signal = extractSignalFromPin(signalPage, pinUrl);
                    if (signal != null) {
                        signals.add(signal);
                        log.info("[PINTEREST-EXPLORE] ✅ Signal extracted — @{} | likes={} | hashtags={} | text={}",
                                signal.getAuthorUsername(),
                                signal.getEngagementScore(),
                                signal.getHashtags().size(),
                                truncate(signal.getRawText(), 60));
                    } else {
                        log.debug("[PINTEREST-EXPLORE] ⚠ Could not extract signal from: {}", pinUrl);
                    }
                } catch (TimeoutError te) {
                    log.warn("[PINTEREST-EXPLORE] Timeout loading pin page — skipping: {}", pinUrl);
                } catch (Exception e) {
                    log.warn("[PINTEREST-EXPLORE] Error extracting signal from {}: {}", pinUrl, e.getMessage());
                }

                RandomDelayUtil.shortDelay();

                if (processed % 10 == 0 && processed < totalPins) {
                    log.info("[PINTEREST-EXPLORE] Processed {} pins — pausing to avoid rate limiting", processed);
                    RandomDelayUtil.delay(8_000, 12_000, "pinterest-rate-limit-pause");
                }
            }

            signalPage.close();
            context.close();

        } catch (Exception e) {
            log.error("[PINTEREST-EXPLORE] Fatal error during extraction: {}", e.getMessage(), e);
        }

        log.info("[PINTEREST-EXPLORE] ════════ Extraction complete: {} signals from {} pins ════════",
                signals.size(), pinUrls.size());
        return signals;
    }

    /**
     * Executes the search, scrolls the grid, and collects Pin URLs.
     */
    private Set<String> collectPinUrlsFromSearch(BrowserContext context, String searchQuery) {
        Set<String> pinUrls = new LinkedHashSet<>();
        Page page = context.newPage();

        String url = String.format(SEARCH_URL_TMPL, searchQuery.replace(" ", "%20"));
        log.info("[PINTEREST-EXPLORE] Navigating → {}", url);

        try {
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(DOM_SETTLE_TIMEOUT));

            if (isLoginPage(page.url())) {
                log.warn("[PINTEREST-EXPLORE] Redirected to login — session expired");
                sessionManager.invalidateSession();
                page.close();
                return pinUrls;
            }

            RandomDelayUtil.longDelay();

            // Stealth scrolling to load the dynamic Pinterest grid
            log.info("[PINTEREST-EXPLORE] Scrolling Pinterest grid {} times to load pins...", SCROLL_COUNT);
            for (int i = 0; i < SCROLL_COUNT; i++) {
                page.evaluate("window.scrollBy(0, window.innerHeight * 1.5)");
                // Human-like delay between scrolls
                RandomDelayUtil.delay(2000, 4000, "pinterest-scroll");
                log.debug("[PINTEREST-EXPLORE] Scroll {}/{} complete.", i + 1, SCROLL_COUNT);
            }

            List<ElementHandle> anchors = page.querySelectorAll("a[href^='/pin/']");
            log.debug("[PINTEREST-EXPLORE] Found {} raw anchor tags matching /pin/", anchors.size());

            for (ElementHandle a : anchors) {
                try {
                    String href = a.getAttribute("href");
                    if (href == null || href.isBlank()) continue;

                    String fullUrl = "https://www.pinterest.com" + href;
                    String cleanUrl = fullUrl.split("\\?")[0];
                    if (!cleanUrl.endsWith("/")) cleanUrl += "/";

                    if (PIN_URL_PATTERN.matcher(cleanUrl).matches()) {
                        pinUrls.add(cleanUrl);
                    }
                } catch (Exception e) {
                    log.trace("[PINTEREST-EXPLORE] Skipping anchor: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[PINTEREST-EXPLORE] Search collection failed: {}", e.getMessage());
        } finally {
            page.close();
        }

        return pinUrls;
    }

    /**
     * Navigates to an individual Pin page and extracts metadata into a TrendSignal.
     */
    private TrendSignal extractSignalFromPin(Page page, String pinUrl) {
        page.navigate(pinUrl, new Page.NavigateOptions().setTimeout(POST_PAGE_TIMEOUT));

        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(POST_PAGE_TIMEOUT));
        } catch (TimeoutError te) {
            log.debug("[PINTEREST-EXPLORE] DOM timeout for pin, attempting extraction anyway: {}", pinUrl);
        }

        if (isLoginPage(page.url())) {
            log.warn("[PINTEREST-EXPLORE] Session expired — redirected to login page");
            sessionManager.invalidateSession();
            return null;
        }

        RandomDelayUtil.delay(500, 1500, "pin-settle");

        String title = null;
        String description = null;
        String mediaUrl = null;
        String authorUsername = null;
        long saves = 0; // Pinterest doesn't always expose saves easily, defaulting to 0 for now unless found

        try {
            // Extract Title
            ElementHandle titleEl = page.querySelector("h1");
            if (titleEl != null) title = titleEl.innerText();

            if (title == null || title.isBlank()) {
                ElementHandle ogTitle = page.querySelector("meta[property='og:title']");
                if (ogTitle != null) title = ogTitle.getAttribute("content");
            }

            // Extract Description
            ElementHandle ogDesc = page.querySelector("meta[property='og:description']");
            if (ogDesc != null) description = ogDesc.getAttribute("content");

            if (description == null || description.isBlank()) {
                // Fallback to DOM parsing for description text if og:description is empty
                List<ElementHandle> spans = page.querySelectorAll("div[data-test-id='pin-description'] span");
                for(ElementHandle span : spans) {
                    String text = span.innerText();
                    if(text != null && text.length() > 5) {
                        description = text;
                        break;
                    }
                }
            }

            // Extract Media URL
            ElementHandle ogImage = page.querySelector("meta[property='og:image']");
            if (ogImage != null) mediaUrl = ogImage.getAttribute("content");

            if(mediaUrl == null || mediaUrl.isBlank()){
                ElementHandle img = page.querySelector("div[data-test-id='pin-visual-wrapper'] img");
                if (img != null) mediaUrl = img.getAttribute("src");
            }

            // Extract Author
            List<ElementHandle> links = page.querySelectorAll("a[href^='/']");
            for (ElementHandle link : links) {
                String href = link.getAttribute("href");
                // Creator links usually look like /username/
                if (href != null && href.length() > 2 && !href.startsWith("/pin/") && !href.startsWith("/search/")) {
                    String possibleUser = href.replaceAll("/", "");
                    if (page.querySelector("div[data-test-id='creator-profile-name']") != null) {
                         authorUsername = possibleUser;
                         break;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("[PINTEREST-EXPLORE] DOM extraction error on {}: {}", pinUrl, e.getMessage());
        }

        String rawText = "";
        if (title != null && !title.isBlank()) rawText += title + " ";
        if (description != null && !description.isBlank()) rawText += description;
        rawText = rawText.trim();

        if (rawText.isBlank() && mediaUrl == null) {
            log.debug("[PINTEREST-EXPLORE] Pin lacks text and media, skipping: {}", pinUrl);
            return null;
        }

        List<String> hashtags = extractHashtags(rawText);

        return TrendSignal.builder()
                .platform(Platform.PINTEREST)
                .sourceUrl(pinUrl)
                .rawText(rawText)
                .hashtags(hashtags)
                .engagementScore(saves)
                .authorUsername(authorUsername != null ? authorUsername : "unknown")
                .mediaUrl(mediaUrl)
                .collectedAt(Instant.now())
                .processedByAi(false)
                .build();
    }

    private void installResourceBlocker(Page page) {
        page.route("**/*", route -> {
            String resourceType = route.request().resourceType();
            if (BLOCKED_RESOURCE_TYPES.contains(resourceType)) {
                route.abort();
            } else {
                route.resume();
            }
        });
        log.debug("[PINTEREST-EXPLORE] Resource blocker installed — blocking: {}", BLOCKED_RESOURCE_TYPES);
    }

    private BrowserContext createContext(Browser browser) {
        return browser.newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(sessionManager.getSessionPath())
                        .setViewportSize(1280, 900)
                        .setUserAgent(USER_AGENT));
    }

    private boolean isLoginPage(String url) {
        return url != null && (url.contains("/login") || url.contains("/signup"));
    }

    private List<String> extractHashtags(String text) {
        List<String> hashtags = new ArrayList<>();
        if (text == null || text.isBlank()) return hashtags;

        Matcher m = HASHTAG_PATTERN.matcher(text);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String tag = m.group(1).toLowerCase();
            if (tag.length() >= 2 && tag.length() <= 50 && seen.add(tag)) {
                hashtags.add(tag);
            }
        }
        return hashtags;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "<null>";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
