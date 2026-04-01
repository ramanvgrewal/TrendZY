package com.trendzy.scraper.website;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.trendzy.scraper.util.RandomDelayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for website product extraction.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Resolve redirect chains (linktree, beacons.ai, etc.) to find the
 *       actual brand website.</li>
 *   <li>Detect whether the site is Shopify or a generic platform.</li>
 *   <li>Delegate to the appropriate parser.</li>
 *   <li>Handle all errors gracefully — always return a list (may be empty).</li>
 * </ol>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebsiteClient {

    private final ShopifyParser  shopifyParser;
    private final GenericParser  genericParser;

    private static final String  USER_AGENT    =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int     LOAD_TIMEOUT  = 20_000;
    private static final int     MAX_PRODUCTS  = 30;

    // Known link-in-bio aggregator domains — we need to find the real link inside
    private static final List<String> AGGREGATOR_DOMAINS = List.of(
            "linktr.ee", "beacons.ai", "solo.to", "bio.site", "bio.link", "campsite.bio", "hoo.be", "linkpop"
    );
    private static final List<String> MARKETPLACE_DOMAINS = List.of(
            "amazon.", "flipkart.", "myntra.", "ajio.", "meesho.", "snapdeal."
    );

    // Shopify detection signals
    private static final List<String> SHOPIFY_SIGNALS = List.of(
            "cdn.shopify.com",
            "myshopify.com",
            "Shopify.theme",
            "/cart.js",
            "window.Shopify"
    );

    // ─────────────────────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Opens the given URL, resolves any aggregator redirects, detects the
     * platform, and extracts raw product data.
     *
     * @param rawUrl     the URL from Instagram bio (may be a linktree, etc.)
     * @param playwright a live {@link Playwright} instance
     * @param brandName  used for logging only
     * @return list of {@link RawProduct}; empty list on any error
     */
    public List<RawProduct> extractProducts(String rawUrl, Playwright playwright, String brandName) {
        List<RawProduct> products = new ArrayList<>();

        if (rawUrl == null || rawUrl.isBlank()) {
            log.warn("[WEBSITE] Null/blank URL for brand @{}", brandName);
            return products;
        }
        rawUrl = normalizeInputUrl(rawUrl);
        if (rawUrl == null) {
            log.warn("[WEBSITE] Invalid URL for brand @{}", brandName);
            return products;
        }
        if (isMarketplace(rawUrl)) {
            log.info("[WEBSITE] Skipping marketplace URL for @{}: {}", brandName, rawUrl);
            return products;
        }

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1280, 900)
                            .setUserAgent(USER_AGENT));

            Page page = context.newPage();

            // ── Navigate and follow redirects ─────────────────
            String resolvedUrl = resolveUrl(page, rawUrl, brandName);
            if (resolvedUrl == null) {
                log.warn("[WEBSITE] Could not resolve URL for @{}: {}", brandName, rawUrl);
                context.close();
                return products;
            }

            log.info("[WEBSITE] Processing {} for brand @{}", resolvedUrl, brandName);

            // ── Platform detection ────────────────────────────
            boolean isShopify = detectShopify(page, resolvedUrl);
            log.info("[WEBSITE] Platform for @{}: {}", brandName, isShopify ? "SHOPIFY" : "GENERIC");

            // ── Extraction ────────────────────────────────────
            if (isShopify) {
                products = shopifyParser.extractProducts(page, extractBaseUrl(resolvedUrl));
            } else {
                products = genericParser.extractProducts(page, extractBaseUrl(resolvedUrl));
            }

            if (products.size() > MAX_PRODUCTS) {
                products = products.subList(0, MAX_PRODUCTS);
            }

            context.close();

        } catch (Exception e) {
            log.error("[WEBSITE] Fatal error for @{} ({}): {}", brandName, rawUrl, e.getMessage(), e);
        }

        log.info("[WEBSITE] {} raw products extracted for @{}", products.size(), brandName);
        return products;
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — URL resolution
    // ─────────────────────────────────────────────────────────────

    /**
     * Navigates to the raw URL and, if it's a link aggregator, tries to find
     * the main shop link within the page.
     *
     * @return the resolved store URL, or {@code null} if resolution failed
     */
    private String resolveUrl(Page page, String rawUrl, String brandName) {
        try {
            page.navigate(rawUrl, new Page.NavigateOptions().setTimeout(LOAD_TIMEOUT));
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(LOAD_TIMEOUT));
            RandomDelayUtil.shortDelay();
        } catch (Exception e) {
            log.warn("[WEBSITE] Failed to load {}: {}", rawUrl, e.getMessage());
            return null;
        }

        String currentUrl = page.url();
        if (currentUrl == null || currentUrl.isBlank()) return null;

        // Check if we landed on an aggregator
        boolean isAggregator = AGGREGATOR_DOMAINS.stream()
                .anyMatch(d -> currentUrl.toLowerCase().contains(d));

        if (!isAggregator) {
            // Direct brand website
            return currentUrl;
        }

        log.debug("[WEBSITE] Aggregator page detected for @{} — scanning for shop link", brandName);

        // Try to find the shop/website link within the aggregator page
        // Look for: "shop", "store", ".in", ".com", "website" in link text or href
        List<String> shopSelectors = List.of(
                "a[href*='shop']", "a[href*='store']", "a[href*='.in']", "a[href*='.com']",
                "a:has-text('shop')", "a:has-text('store')", "a:has-text('website')", "a:has-text('visit')"
        );

        for (String sel : shopSelectors) {
            try {
                List<ElementHandle> anchors = page.querySelectorAll(sel);
                for (ElementHandle a : anchors) {
                    String href = a.getAttribute("href");
                    String text = a.innerText();
                    if (href != null && href.startsWith("http")
                            && !isKnownSocialMedia(href)
                            && !AGGREGATOR_DOMAINS.stream().anyMatch(d -> href.contains(d))) {
                        
                        log.debug("[WEBSITE] Found potential shop link from aggregator: {} (text: {})", href, text);
                        return href.split("\\?")[0];
                    }
                }
            } catch (Exception ignored) {}
        }

        // Use more generic scan if selectors failed
        try {
            List<ElementHandle> all = page.querySelectorAll("a[href^='http']");
            for (ElementHandle a : all) {
                String href = a.getAttribute("href");
                if (href != null && !isKnownSocialMedia(href) 
                    && !AGGREGATOR_DOMAINS.stream().anyMatch(d -> href.contains(d))) {
                    return href.split("\\?")[0];
                }
            }
        } catch (Exception ignored) {}

        return null; // Skip this brand if no store link found
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — platform detection
    // ─────────────────────────────────────────────────────────────

    private boolean detectShopify(Page page, String resolvedUrl) {
        // Check URL first (fastest)
        if (resolvedUrl.contains("myshopify.com")) return true;

        // Check page source for Shopify signals
        try {
            String html = page.content();
            if (html != null) {
                for (String signal : SHOPIFY_SIGNALS) {
                    if (html.contains(signal)) return true;
                }
            }
        } catch (Exception e) {
            log.trace("[WEBSITE] Shopify detection error: {}", e.getMessage());
        }

        // Try hitting /products.json — Shopify responds with JSON; others return 404/HTML
        try {
            String base = extractBaseUrl(resolvedUrl);
            String testUrl = base + "/products.json?limit=1";
            page.navigate(testUrl, new Page.NavigateOptions().setTimeout(8_000));
            String body = page.innerText("body");
            if (body != null && body.trim().startsWith("{") && body.contains("\"products\"")) {
                // Restore the original page for extraction
                page.navigate(resolvedUrl, new Page.NavigateOptions().setTimeout(LOAD_TIMEOUT));
                return true;
            }
            // Restore page
            page.navigate(resolvedUrl, new Page.NavigateOptions().setTimeout(LOAD_TIMEOUT));
        } catch (Exception e) {
            log.trace("[WEBSITE] /products.json probe failed: {}", e.getMessage());
        }

        return false;
    }

    private String normalizeInputUrl(String rawUrl) {
        String trimmed = rawUrl == null ? null : rawUrl.trim();
        if (trimmed == null || trimmed.isBlank()) return null;
        if (!trimmed.startsWith("http")) trimmed = "https://" + trimmed;

        if (trimmed.startsWith("https://l.instagram.com/") || trimmed.startsWith("http://l.instagram.com/")) {
            try {
                java.net.URI uri = java.net.URI.create(trimmed);
                String query = uri.getRawQuery();
                if (query != null) {
                    String encoded = Arrays.stream(query.split("&"))
                            .filter(p -> p.startsWith("u="))
                            .map(p -> p.substring(2))
                            .findFirst()
                            .orElse(null);
                    if (encoded != null) {
                        String decoded = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
                        if (decoded.startsWith("http")) trimmed = decoded;
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            java.net.URI uri = java.net.URI.create(trimmed);
            String host = uri.getHost();
            if (host == null) return null;
            String path = uri.getPath() == null ? "" : uri.getPath();
            return uri.getScheme() + "://" + host + path;
        } catch (Exception e) {
            return trimmed.split("\\?")[0];
        }
    }

    private boolean isMarketplace(String url) {
        String lower = url.toLowerCase();
        return MARKETPLACE_DOMAINS.stream().anyMatch(lower::contains);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — helpers
    // ─────────────────────────────────────────────────────────────

    private String extractBaseUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private boolean isKnownSocialMedia(String url) {
        List<String> social = List.of(
                "instagram.com", "facebook.com", "twitter.com",
                "tiktok.com", "youtube.com", "pinterest.com",
                "snapchat.com", "whatsapp.com"
        );
        return social.stream().anyMatch(url.toLowerCase()::contains);
    }
}
