package com.trendzy.ingestion.scraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebsiteClient {

    private final ShopifyParser shopifyParser;
    private final GenericParser genericParser;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int LOAD_TIMEOUT = 20_000;
    private static final int MAX_PRODUCTS = 30;

    private static final List<String> AGGREGATOR_DOMAINS = List.of(
            "linktr.ee", "beacons.ai", "solo.to", "bio.site", "bio.link", "campsite.bio", "hoo.be", "linkpop"
    );
    private static final List<String> MARKETPLACE_DOMAINS = List.of(
            "amazon.", "flipkart.", "myntra.", "ajio.", "meesho.", "snapdeal."
    );
    private static final List<String> SHOPIFY_SIGNALS = List.of(
            "cdn.shopify.com", "myshopify.com", "Shopify.theme", "/cart.js", "window.Shopify"
    );

    public List<RawProduct> extractProducts(String rawUrl, Playwright playwright, String brandName) {
        List<RawProduct> products = new ArrayList<>();

        if (rawUrl == null || rawUrl.isBlank()) return products;

        rawUrl = normalizeInputUrl(rawUrl);
        if (rawUrl == null || isMarketplace(rawUrl)) return products;

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions().setViewportSize(1280, 900).setUserAgent(USER_AGENT));

            Page page = context.newPage();

            String resolvedUrl = resolveUrl(page, rawUrl, brandName);
            if (resolvedUrl == null) {
                context.close();
                return products;
            }

            boolean isShopify = detectShopify(page, resolvedUrl);
            log.info("[WEBSITE] Platform for @{}: {}", brandName, isShopify ? "SHOPIFY" : "GENERIC");

            if (isShopify) {
                products = shopifyParser.extractProducts(page, extractBaseUrl(resolvedUrl), resolvedUrl);
            } else {
                products = genericParser.extractProducts(page, extractBaseUrl(resolvedUrl));
            }

            if (products.size() > MAX_PRODUCTS) products = products.subList(0, MAX_PRODUCTS);
            context.close();

        } catch (Exception e) {
            log.error("[WEBSITE] Fatal error for @{} ({}): {}", brandName, rawUrl, e.getMessage());
        }
        return products;
    }

    private String resolveUrl(Page page, String rawUrl, String brandName) {
        try {
            page.navigate(rawUrl, new Page.NavigateOptions().setTimeout(LOAD_TIMEOUT));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000); // Playwright native wait
        } catch (Exception e) {
            return null;
        }

        String currentUrl = page.url();
        boolean isAggregator = AGGREGATOR_DOMAINS.stream().anyMatch(d -> currentUrl.toLowerCase().contains(d));

        if (!isAggregator) return currentUrl;

        List<String> shopSelectors = List.of(
                "a[href*='shop']", "a[href*='store']", "a[href*='.in']", "a[href*='.com']",
                "a:has-text('shop')", "a:has-text('store')", "a:has-text('website')"
        );

        for (String sel : shopSelectors) {
            try {
                List<ElementHandle> anchors = page.querySelectorAll(sel);
                for (ElementHandle a : anchors) {
                    String href = a.getAttribute("href");
                    if (href != null && href.startsWith("http") && !isKnownSocialMedia(href)
                            && AGGREGATOR_DOMAINS.stream().noneMatch(href::contains)) {
                        return href.split("\\?")[0];
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean detectShopify(Page page, String resolvedUrl) {
        if (resolvedUrl.contains("myshopify.com")) return true;
        try {
            String html = page.content();
            for (String signal : SHOPIFY_SIGNALS) {
                if (html.contains(signal)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String normalizeInputUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (!trimmed.startsWith("http")) trimmed = "https://" + trimmed;
        return trimmed.split("\\?")[0];
    }

    private boolean isMarketplace(String url) {
        return MARKETPLACE_DOMAINS.stream().anyMatch(url.toLowerCase()::contains);
    }

    private String extractBaseUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private boolean isKnownSocialMedia(String url) {
        List<String> social = List.of("instagram.com", "facebook.com", "twitter.com", "tiktok.com", "youtube.com");
        return social.stream().anyMatch(url.toLowerCase()::contains);
    }
}