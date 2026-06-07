package com.trendzy.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.trendzy.dto.response.ProductCandidate;
import com.trendzy.dto.response.ProductCandidate.Platform;
import com.trendzy.model.mongo.ProductFingerprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multi-platform product scraper using Playwright.
 *
 * Platform order: Amazon → Flipkart → Meesho → Myntra
 * Rationale: Amazon and Flipkart are the most bot-tolerant. Meesho is usually
 * fine. Myntra actively fingerprints HTTP/2 traffic — it is tried last.
 *
 * Key fixes vs previous version:
 *   1. --disable-http2 Chrome flag → prevents ERR_HTTP2_PROTOCOL_ERROR on Myntra
 *   2. page.setDefaultTimeout(PAGE_TIMEOUT_MS) → hard cap on ALL page operations
 *   3. Myntra uses DOMCONTENTLOADED not NETWORKIDLE (NETWORKIDLE can hang forever)
 *   4. Richer stealth headers (sec-ch-ua, sec-fetch-*, Accept-Encoding)
 *   5. Platform order changed so pipeline still produces results if Myntra fails
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformScraperService {

    private final ImageExtractionService imageExtractionService;

    /** Hard cap on every page.navigate() / waitForLoadState() call. */
    private static final int PAGE_TIMEOUT_MS         = 12_000;
    private static final int MAX_PRODUCTS_PER_PLATFORM = 5;
    private static final int POLITE_DELAY_MS          = 700;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36";

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public List<ProductCandidate> fetchAllCandidates(ProductFingerprint fingerprint,
                                                     String productName) {
        List<ProductCandidate> all = new ArrayList<>();
        String query = buildSearchQuery(fingerprint, productName);

        log.info("[SCRAPER] Fetching candidates for query: '{}'", query);

        try (Playwright playwright = Playwright.create()) {

            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-blink-features=AutomationControlled",
                            // ── KEY FIX: disables HTTP/2 which Myntra uses for bot detection ──
                            "--disable-http2",
                            "--disable-features=IsolateOrigins,site-per-process",
                            "--disable-web-security"
                    ));

            try (Browser browser = playwright.chromium().launch(opts)) {

                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions()
                                .setUserAgent(USER_AGENT)
                                .setViewportSize(1366, 768)
                                .setLocale("en-IN")
                                .setTimezoneId("Asia/Kolkata")
                                .setExtraHTTPHeaders(Map.of(
                                        "Accept-Language",  "en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7",
                                        "Accept-Encoding",  "gzip, deflate, br",
                                        "Accept",           "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                                        "sec-ch-ua",        "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"",
                                        "sec-ch-ua-mobile", "?0",
                                        "sec-ch-ua-platform","\"Windows\"",
                                        "Upgrade-Insecure-Requests", "1"
                                ))
                );

                // Spoof navigator.webdriver = false at page creation
                context.addInitScript("Object.defineProperty(navigator,'webdriver',{get:()=>undefined})");

                // Platform order: most reliable → least reliable
                // If Amazon/Flipkart/Meesho give enough results, Myntra failure doesn't block us
                all.addAll(fetchFromPlatform(context, Platform.AMAZON,   query));
                all.addAll(fetchFromPlatform(context, Platform.FLIPKART, query));
                all.addAll(fetchFromPlatform(context, Platform.MEESHO,   query));
                all.addAll(fetchFromPlatform(context, Platform.MYNTRA,   query));
            }

        } catch (Exception e) {
            log.error("[SCRAPER] Playwright fatal error for query '{}': {}", query, e.getMessage());
        }

        log.info("[SCRAPER] Total candidates fetched: {}", all.size());
        return all;
    }

    // ─────────────────────────────────────────────────────────────
    // PER-PLATFORM FETCH
    // ─────────────────────────────────────────────────────────────

    private List<ProductCandidate> fetchFromPlatform(BrowserContext context,
                                                     Platform platform,
                                                     String query) {
        List<ProductCandidate> candidates = new ArrayList<>();
        Page page = null;

        try {
            page = context.newPage();

            // Hard cap on ALL page operations — prevents any single platform
            // from hanging the entire pipeline (the original root cause of the 50s stall)
            page.setDefaultTimeout(PAGE_TIMEOUT_MS);

            String searchUrl = buildSearchUrl(platform, query);
            log.debug("[SCRAPER] {} → {}", platform, truncate(searchUrl));

            navigateForPlatform(page, platform, searchUrl);

            String html = page.content();
            Document searchDoc = Jsoup.parse(html, searchUrl);

            List<String> productLinks = extractProductLinks(platform, searchDoc, searchUrl);
            log.debug("[SCRAPER] {} product links on {}", productLinks.size(), platform);

            for (String link : productLinks) {
                try {
                    navigateForPlatform(page, platform, link);
                    String productHtml = page.content();
                    Document productDoc = Jsoup.parse(productHtml, link);

                    String title = extractTitle(productDoc);
                    String image = imageExtractionService.extractImage(productDoc, link);

                    ProductCandidate.ProductCandidateBuilder pb = ProductCandidate.builder()
                            .platform(platform)
                            .title(title != null ? title : "")
                            .url(link)
                            .imageUrl(image);

                    extractPricing(productDoc, platform, pb);

                    candidates.add(pb.build());

                    log.debug("[SCRAPER] {} candidate: '{}' image={} price={}",
                            platform, truncate(title), image != null ? "✓" : "✗",
                            pb.build().getPrice());

                    Thread.sleep(POLITE_DELAY_MS);

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.debug("[SCRAPER] {} product page failed: {}", platform, e.getMessage());
                }
            }

        } catch (Exception e) {
            // Log at WARN not ERROR — a single platform failing is expected and handled
            log.warn("[SCRAPER] {} fetch failed (continuing with other platforms): {}",
                    platform, extractRootCause(e));
        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
            }
        }

        return candidates;
    }

    // ─────────────────────────────────────────────────────────────
    // NAVIGATION — platform-specific wait strategies
    // ─────────────────────────────────────────────────────────────

    private void navigateForPlatform(Page page, Platform platform, String url) {
        Page.NavigateOptions navOpts = new Page.NavigateOptions()
                .setTimeout(PAGE_TIMEOUT_MS);

        switch (platform) {

            case AMAZON -> {
                page.navigate(url, navOpts);
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                // Dismiss sign-in nag if present
                try {
                    page.click("#nav-signin-tooltip .nav-action-button",
                            new Page.ClickOptions().setTimeout(2000));
                } catch (Exception ignored) {}
            }

            case FLIPKART -> {
                page.navigate(url, navOpts);
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                // Dismiss login modal
                try {
                    page.click("button._2KpZ6l._2doB4z",
                            new Page.ClickOptions().setTimeout(2000));
                } catch (Exception ignored) {}
            }

            case MEESHO -> {
                page.navigate(url, navOpts);
                // NETWORKIDLE is fine for Meesho — it doesn't block scrapers
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(PAGE_TIMEOUT_MS));
                } catch (Exception e) {
                    // Fallback to DOMCONTENTLOADED if NETWORKIDLE times out
                    log.debug("[SCRAPER] Meesho NETWORKIDLE timeout — falling back to DOMCONTENTLOADED");
                }
            }

            case MYNTRA -> {
                // ── Myntra: use DOMCONTENTLOADED only ──
                // Reason: Myntra's React SPA triggers NETWORKIDLE very late (or never)
                // when bot detection intercepts the request. DOMCONTENTLOADED returns
                // faster and still gives us enough HTML to extract links.
                page.navigate(url, navOpts);
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                // Extra 500ms for basic JS render — cheap and helps extract product links
                try { Thread.sleep(500); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH URL BUILDER
    // ─────────────────────────────────────────────────────────────

    private String buildSearchUrl(Platform platform, String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return switch (platform) {
            case AMAZON   -> "https://www.amazon.in/s?k=" + encoded;
            case FLIPKART -> "https://www.flipkart.com/search?q=" + encoded;
            case MEESHO   -> "https://www.meesho.com/search?q=" + encoded;
            // Myntra URL-path format (their canonical search pattern)
            case MYNTRA   -> "https://www.myntra.com/" + query.toLowerCase()
                    .replaceAll("[^a-z0-9 ]", "")
                    .trim()
                    .replace(" ", "-");
        };
    }

    // ─────────────────────────────────────────────────────────────
    // PRODUCT LINK EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private List<String> extractProductLinks(Platform platform, Document doc, String searchUrl) {
        return switch (platform) {
            case AMAZON   -> extractAmazonLinks(doc, searchUrl);
            case FLIPKART -> extractFlipkartLinks(doc, searchUrl);
            case MEESHO   -> extractMeeshoLinks(doc);
            case MYNTRA   -> extractMyntraLinks(doc);
        };
    }

    private List<String> extractAmazonLinks(Document doc, String baseUrl) {
        List<String> links = new ArrayList<>();
        Elements els = doc.select(
                "div[data-component-type=s-search-result] h2 a.a-link-normal[href*=/dp/]");
        if (els.isEmpty()) els = doc.select("a[href*=/dp/]");

        for (Element a : els) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = resolveHref(a, baseUrl);
            if (href != null && href.contains("/dp/")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    private List<String> extractFlipkartLinks(Document doc, String baseUrl) {
        List<String> links = new ArrayList<>();
        // Flipkart uses /p/ for product detail pages
        Elements els = doc.select("a[href*=/p/]");
        for (Element a : els) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = resolveHref(a, baseUrl);
            if (href != null && href.contains("/p/")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    private List<String> extractMeeshoLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements els = doc.select("a[href*=/product/]");
        for (Element a : els) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = a.attr("abs:href");
            if (!href.isBlank() && href.contains("meesho.com")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    private List<String> extractMyntraLinks(Document doc) {
        List<String> links = new ArrayList<>();
        // Try multiple Myntra selectors — their DOM can vary
        Elements els = doc.select("a[href*=/buy/], li.product-base a[href^=/]");
        if (els.isEmpty()) {
            els = doc.select("a[href^=/][href*=-][href*=buy]");
        }
        if (els.isEmpty()) {
            // Fallback: grab any Myntra product-like path
            els = doc.select("a[href^=/]");
        }

        for (Element a : els) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;
            if (href.startsWith("/")) href = "https://www.myntra.com" + href.split("\\?")[0];
            else href = href.split("\\?")[0];
            // Only real product pages (contain a numeric ID segment)
            if (href.contains("myntra.com")
                    && !href.contains("/search")
                    && !href.contains("/cart")
                    && !href.contains("/account")
                    && href.matches(".*\\/[0-9]+$")) {
                links.add(href);
            }
        }
        return links;
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BUILDER
    // ─────────────────────────────────────────────────────────────

    private String buildSearchQuery(ProductFingerprint fp, String productName) {
        if (fp == null) return sanitize(productName);

        StringBuilder q = new StringBuilder();
        if (notBlank(fp.getBrand()))       q.append(fp.getBrand()).append(" ");
        if (notBlank(fp.getProductType())) q.append(fp.getProductType()).append(" ");
        if (notBlank(fp.getColor()))       q.append(fp.getColor()).append(" ");
        if (notBlank(fp.getGender()) && !fp.getGender().equalsIgnoreCase("unisex"))
            q.append(fp.getGender()).append(" ");

        String query = q.toString().trim();
        return query.length() < 4 ? sanitize(productName) : query;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private String extractTitle(Document doc) {
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String c = ogTitle.attr("content");
            if (!c.isBlank()) return c.trim();
        }
        String title = doc.title();
        return !title.isBlank() ? title.trim() : null;
    }

    private String resolveHref(Element a, String baseUrl) {
        String href = a.attr("abs:href");
        if (href.isBlank()) href = a.attr("href");
        if (href.isBlank()) return null;
        if (href.startsWith("http")) return href;
        try {
            java.net.URI base = java.net.URI.create(baseUrl);
            return base.getScheme() + "://" + base.getHost()
                    + (href.startsWith("/") ? "" : "/") + href;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts just the first line of an exception message — avoids the massive
     * Playwright stack traces polluting warn-level logs.
     */
    private static String extractRootCause(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        int nl = msg.indexOf('\n');
        return nl > 0 ? msg.substring(0, nl).trim() : msg;
    }

    private static String truncate(String s) {
        if (s == null || s.length() <= 70) return s;
        return s.substring(0, 70) + "...";
    }

    private void extractPricing(Document doc, Platform platform, ProductCandidate.ProductCandidateBuilder builder) {
        Double price = null;
        Double originalPrice = null;

        try {
            switch (platform) {
                case AMAZON -> {
                    Element pEl = doc.selectFirst(".a-price-whole");
                    if (pEl != null) price = parsePrice(pEl.text());
                    Element opEl = doc.selectFirst(".a-text-price span[aria-hidden=true]");
                    if (opEl != null) originalPrice = parsePrice(opEl.text());
                }
                case FLIPKART -> {
                    Element pEl = doc.selectFirst("div._30jeq3, div.Nx9bqj");
                    if (pEl != null) price = parsePrice(pEl.text());
                    Element opEl = doc.selectFirst("div._3I9_wc, div.yRaY8j");
                    if (opEl != null) originalPrice = parsePrice(opEl.text());
                }
                case MYNTRA -> {
                    Element pEl = doc.selectFirst(".pdp-price, strong.pdp-price");
                    if (pEl != null) price = parsePrice(pEl.text());
                    Element opEl = doc.selectFirst(".pdp-mrp, s.pdp-mrp");
                    if (opEl != null) originalPrice = parsePrice(opEl.text());
                }
                case MEESHO -> {
                    Element pEl = doc.selectFirst("h4");
                    if (pEl != null) price = parsePrice(pEl.text());
                }
            }
        } catch (Exception e) {
            log.debug("[SCRAPER] Failed to parse pricing on {}: {}", platform, e.getMessage());
        }

        if (price != null) builder.price(price);
        if (originalPrice != null) builder.originalPrice(originalPrice);

        if (price != null && originalPrice != null && originalPrice > price) {
            double discount = ((originalPrice - price) / originalPrice) * 100.0;
            builder.discount(Math.round(discount * 10.0) / 10.0);
        }
    }

    private Double parsePrice(String text) {
        if (text == null || text.isBlank()) return null;
        String clean = text.replaceAll("[^0-9.]", "");
        if (clean.isBlank()) return null;
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}