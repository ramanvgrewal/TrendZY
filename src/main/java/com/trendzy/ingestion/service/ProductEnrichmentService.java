package com.trendzy.ingestion.service;

import com.microsoft.playwright.*;
import com.trendzy.ingestion.model.Trend;
import com.trendzy.ingestion.model.TrendSignal;
import com.trendzy.ingestion.repository.TrendRepository;
import com.trendzy.ingestion.repository.TrendSignalRepository;
import com.trendzy.ingestion.scraper.WebsiteClient;
import com.trendzy.ingestion.scraper.RawProduct;
import com.trendzy.ingestion.scraper.InstagramBioExtractor;
import com.trendzy.ingestion.scraper.instagram.InstagramSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEnrichmentService {

    private final TrendRepository trendRepository;
    private final TrendSignalRepository signalRepository;
    private final WebsiteClient websiteClient;
    private final InstagramSessionManager sessionManager;
    private final InstagramBioExtractor instagramBioExtractor;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Guard against overlapping scheduler ticks — a long batch must not double-run.
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "60000")
    public void runEnrichmentBatch() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("[PLAYWRIGHT ENRICH] 🚨 Previous batch still running — skipping this tick.");
            return;
        }
        try {
            processEnrichmentBatch();
        } finally {
            isRunning.set(false);
        }
    }

    private void processEnrichmentBatch() {
        List<Trend> pendingTrends = trendRepository.findPendingEnrichment();
        if (pendingTrends.isEmpty()) return;

        log.info("[PLAYWRIGHT ENRICH] Starting batch processing for {} trends...", pendingTrends.size());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            sessionManager.ensureSession(playwright);

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT)
                    .setViewportSize(1280, 900);

            if (sessionManager.sessionExists()) {
                contextOptions.setStorageStatePath(sessionManager.getSessionPath());
            }

            try (BrowserContext context = browser.newContext(contextOptions)) {
                for (Trend trend : pendingTrends) {
                    try {
                        enrichSingleTrend(trend, context, playwright);
                    } catch (Exception e) {
                        log.error("[PLAYWRIGHT ENRICH] Failed for trend {}: {}", trend.getTrendName(), e.getMessage());
                        trend.setEnrichmentStatus("FAILED");
                        trendRepository.save(trend);
                    }
                }
            }
            browser.close();
        } catch (Exception e) {
            log.error("[PLAYWRIGHT ENRICH] Fatal tracking engine error: {}", e.getMessage(), e);
        }
    }

    private void enrichSingleTrend(Trend trend, BrowserContext context, Playwright playwright) {
        log.info("[PLAYWRIGHT ENRICH] Hunting targets for trend: '{}'", trend.getTrendName());
        trend.setEnrichmentStatus("PROCESSING");
        trendRepository.save(trend);

        // 1. Genuine Underdog Hunt
        Trend.ProductDetail underdog = huntUnderdog(trend, context, playwright);

        // 2. High-Volume Marketplace Sniper Run
        String query = trend.getEnrichmentQuery();
        if (query == null || query.isBlank()) query = trend.getTrendName();
        Trend.ProductDetail amazon = scrapeAmazon(query, context);
        Trend.ProductDetail flipkart = scrapeFlipkart(query, context);

        // 3. Assemble and Persist Package
        Trend.ScrapedProducts products = Trend.ScrapedProducts.builder()
                .underdog(underdog)
                .amazon(amazon)
                .flipkart(flipkart)
                .build();

        trend.setProducts(products);
        trend.setEnrichmentStatus("COMPLETED");
        trend.setLastUpdatedAt(LocalDateTime.now());
        trendRepository.save(trend);
        log.info("[PLAYWRIGHT ENRICH] ✅ Successfully completed enrichment for trend: '{}'", trend.getTrendName());
    }

    private Trend.ProductDetail huntUnderdog(Trend trend, BrowserContext context, Playwright playwright) {
        if (trend.getSupportingSignalIds() == null || trend.getSupportingSignalIds().isEmpty()) return null;

        // Trace back the raw documents via Mongo repositories
        for (String signalId : trend.getSupportingSignalIds()) {
            try {
                var signalOpt = signalRepository.findById(signalId);
                if (signalOpt.isEmpty()) continue;
                var signal = signalOpt.get();

                String outLink = null;
                String platform = signal.getPlatform() != null ? signal.getPlatform().name() : "";

                if ("INSTAGRAM".equalsIgnoreCase(platform)) {
                    outLink = instagramBioExtractor.extractBioLink(signal.getAuthorUsername(), context);
                }

                if (outLink != null && !outLink.isBlank()) {
                    List<RawProduct> products = websiteClient.extractProducts(outLink, playwright, signal.getAuthorUsername());
                    if (!products.isEmpty()) {
                        RawProduct winner = products.stream()
                                .filter(p -> p.getMainPrice() != null && p.getMainPrice() >= 300 && p.getMainPrice() <= 5999)
                                .max(Comparator.comparingInt(p -> calculateMatchScore(p.getProductName(), trend.getTrendName())))
                                .orElse(null);

                        if (winner != null) {
                            Integer sellingPrice = winner.getMainPrice() != null ? winner.getMainPrice().intValue() : null;
                            Integer mrpPrice = winner.getOriginalPrice() != null ? winner.getOriginalPrice().intValue() : sellingPrice;
                            return Trend.ProductDetail.builder()
                                    .brandName(signal.getAuthorUsername())
                                    .title(winner.getProductName())
                                    .price(sellingPrice)
                                    .originalPrice(mrpPrice)
                                    .shopUrl(winner.getProductUrl())
                                    .imageUrl(winner.getImageUrl())
                                    .codAvailable(true)
                                    .build();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[PLAYWRIGHT ENRICH] Underdog parsing route failed for current signal segment: {}", e.getMessage());
            }
        }
        return null;
    }

    private int calculateMatchScore(String productName, String trendName) {
        if (productName == null || productName.isBlank()) return 0;
        if (trendName == null || trendName.isBlank()) return 10;
        String p = productName.toLowerCase();
        String t = trendName.toLowerCase();
        int score = 0;
        for (String word : t.split("\\s+")) {
            if (word.length() > 1 && p.contains(word)) score += 10;
        }
        if (p.contains(t)) score += 50;
        return score;
    }

    // ── Amazon India — Hyper-Aggressive Fallback Selectors ──

    private Trend.ProductDetail scrapeAmazon(String query, BrowserContext context) {
        if (query == null || query.isBlank()) return null;
        try (Page page = context.newPage()) {
            page.navigate("https://www.amazon.in/s?k=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            page.waitForLoadState();
            page.waitForTimeout(2000);

            // CAPTCHA detection
            String pageTitle = page.title();
            if (pageTitle != null && pageTitle.toLowerCase().contains("captcha")) {
                log.warn("[AMAZON] CAPTCHA detected for query: {}", query);
                return null;
            }

            // Multi-fallback tile selectors for DOM structure changes
            Locator firstTile = null;
            String[] tileSelectors = {
                    "div[data-component-type='s-search-result']",
                    "div.s-result-item[data-asin]:not([data-asin=''])",
                    "div.sg-col-inner .s-result-item",
                    "[data-cel-widget^='search_result_']"
            };
            for (String sel : tileSelectors) {
                Locator loc = page.locator(sel).first();
                try {
                    if (loc.count() > 0) { firstTile = loc; break; }
                } catch (Exception ignored) {}
            }
            if (firstTile == null) {
                log.warn("[AMAZON] No search result tiles found for query: {}", query);
                return null;
            }

            // Title extraction — cascading selectors
            String title = extractText(firstTile, "h2 a span", "h2 span", "h2", "span.a-text-normal");
            if (title == null || title.isBlank()) {
                log.warn("[AMAZON] No title found for query: {}", query);
                return null;
            }

            // Selling price — cascading selectors
            String priceStr = extractText(firstTile,
                    "span.a-price:not([data-a-strike]) span.a-offscreen",
                    "span.a-price-whole",
                    "span.a-price span.a-offscreen",
                    "span.a-color-price");
            Integer sellingPrice = parsePriceClean(priceStr);

            // Original/MRP price — crossed-out price selectors
            String originalPriceStr = extractText(firstTile,
                    "span.a-price[data-a-strike] span.a-offscreen",
                    "span.a-text-price span.a-offscreen",
                    "span.a-text-price",
                    "span.priceBlockStrikePriceString");
            Integer originalPrice = parsePriceClean(originalPriceStr);
            if (originalPrice == null) originalPrice = sellingPrice;

            // Link extraction
            String link = null;
            String[] linkSelectors = {
                    "h2 a.a-link-normal",
                    "a.a-link-normal.s-no-outline",
                    "a.a-link-normal[href*='/dp/']",
                    ".s-product-image-container a"
            };
            for (String sel : linkSelectors) {
                try {
                    Locator loc = firstTile.locator(sel).first();
                    if (loc.count() > 0) {
                        String href = loc.getAttribute("href");
                        if (href != null && !href.isBlank()) {
                            link = href.startsWith("http") ? href : "https://www.amazon.in" + href;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Image extraction
            String imgUrl = null;
            try {
                imgUrl = firstTile.locator("img.s-image").getAttribute("src");
                if (imgUrl == null) imgUrl = firstTile.locator("img").first().getAttribute("src");
            } catch (Exception ignored) {}

            return Trend.ProductDetail.builder()
                    .title(title)
                    .price(sellingPrice)
                    .originalPrice(originalPrice)
                    .shopUrl(link)
                    .imageUrl(imgUrl)
                    .brandName("Amazon")
                    .codAvailable(true)
                    .build();
        } catch (Exception e) {
            log.warn("[AMAZON] Scrape failed for query '{}': {}", query, e.getMessage());
            return null;
        }
    }

    // ── Flipkart — Dual Pricing with Fallback Selectors ──

    private Trend.ProductDetail scrapeFlipkart(String query, BrowserContext context) {
        if (query == null || query.isBlank()) return null;
        try (Page page = context.newPage()) {
            page.navigate("https://www.flipkart.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            page.waitForLoadState();
            page.waitForTimeout(2000);

            String pageTitle = page.title();
            if (pageTitle != null && pageTitle.toLowerCase().contains("captcha")) {
                log.warn("[FLIPKART] CAPTCHA detected for query: {}", query);
                return null;
            }

            // Multi-fallback tile selectors
            Locator firstTile = null;
            String[] tileSelectors = {
                    "a.CGtC98",
                    "a._1fQZEK",
                    "a[href*='/p/']",
                    "div._1AtVbE a",
                    "div._4ddWXP a"
            };
            for (String sel : tileSelectors) {
                Locator loc = page.locator(sel).first();
                try {
                    if (loc.count() > 0) { firstTile = loc; break; }
                } catch (Exception ignored) {}
            }
            if (firstTile == null) {
                log.warn("[FLIPKART] No search result tiles found for query: {}", query);
                return null;
            }

            // Link
            String href = firstTile.getAttribute("href");
            String link = (href != null)
                    ? (href.startsWith("http") ? href : "https://www.flipkart.com" + href)
                    : null;

            // Title & Image
            String title = null;
            String imgUrl = null;
            try {
                Locator imgLoc = firstTile.locator("img").first();
                if (imgLoc.count() > 0) {
                    title = imgLoc.getAttribute("alt");
                    imgUrl = imgLoc.getAttribute("src");
                }
            } catch (Exception ignored) {}
            // Fallback title
            if (title == null || title.isBlank()) {
                title = extractText(firstTile, ".syl9yP", ".KzDlHZ", ".IRpw9B", "div.KzDlHZ", "div._4rR01T", "a.IRpwTa", "div.s1Q9rs");
            }

            // Selling price
            String priceStr = extractText(firstTile, ".Nx9bqj", "._30jeq3", ".hl05eU", "div.Nx9bqj", "div._30jeq3", "div._25b18c span");
            Integer sellingPrice = parsePriceClean(priceStr);

            // Original/MRP price — struck-through
            String originalPriceStr = extractText(firstTile, ".yRaY8j", "._3I9_wc", "div.yRaY8j", "div._3I9_wc", "div._27UcVY");
            Integer originalPrice = parsePriceClean(originalPriceStr);
            if (originalPrice == null) originalPrice = sellingPrice;

            return Trend.ProductDetail.builder()
                    .title(title)
                    .price(sellingPrice)
                    .originalPrice(originalPrice)
                    .shopUrl(link)
                    .imageUrl(imgUrl)
                    .brandName("Flipkart")
                    .codAvailable(true)
                    .build();
        } catch (Exception e) {
            log.warn("[FLIPKART] Scrape failed for query '{}': {}", query, e.getMessage());
            return null;
        }
    }

    // ── Utility: Cascading text extraction from multiple selectors ──

    private String extractText(Locator parent, String... selectors) {
        for (String selector : selectors) {
            try {
                Locator loc = parent.locator(selector).first();
                if (loc.count() > 0) {
                    String text = loc.innerText();
                    if (text != null && !text.isBlank()) return text.trim();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ── Price parser: strips ₹, commas, dots — returns null if not found ──

    private Integer parsePriceClean(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) return null;
        try {
            String clean = priceStr.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) return null;
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}