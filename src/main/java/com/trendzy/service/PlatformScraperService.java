package com.trendzy.service;

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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-platform product scraper.
 * For each platform, builds a search URL from the product fingerprint,
 * extracts up to 5 product links, and returns ProductCandidate objects
 * with title and image populated.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformScraperService {

    private final ImageExtractionService imageExtractionService;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_PRODUCTS_PER_PLATFORM = 5;

    // ─────────────────────────────────────────────────────────────
    // PUBLIC — Fetch candidates from all platforms
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetch product candidates from all platforms for the given fingerprint.
     * Platform priority: Myntra → Amazon → Flipkart → Meesho
     */
    public List<ProductCandidate> fetchAllCandidates(ProductFingerprint fingerprint, String productName) {
        List<ProductCandidate> all = new ArrayList<>();
        String query = buildSearchQuery(fingerprint, productName);

        log.info("[SCRAPER] Fetching candidates for query: '{}'", query);

        all.addAll(fetchFromPlatform(Platform.MYNTRA, query));
        all.addAll(fetchFromPlatform(Platform.AMAZON, query));
        all.addAll(fetchFromPlatform(Platform.FLIPKART, query));
        all.addAll(fetchFromPlatform(Platform.MEESHO, query));

        log.info("[SCRAPER] Total candidates fetched: {}", all.size());
        return all;
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BUILDER
    // ─────────────────────────────────────────────────────────────

    private String buildSearchQuery(ProductFingerprint fp, String productName) {
        if (fp == null) {
            return sanitize(productName);
        }

        StringBuilder q = new StringBuilder();
        if (fp.getBrand() != null && !fp.getBrand().isBlank()) {
            q.append(fp.getBrand()).append(" ");
        }
        if (fp.getProductType() != null && !fp.getProductType().isBlank()) {
            q.append(fp.getProductType()).append(" ");
        }
        if (fp.getColor() != null && !fp.getColor().isBlank()) {
            q.append(fp.getColor()).append(" ");
        }
        if (fp.getGender() != null && !fp.getGender().isBlank()
                && !fp.getGender().equalsIgnoreCase("unisex")) {
            q.append(fp.getGender()).append(" ");
        }

        String query = q.toString().trim();
        if (query.length() < 5) {
            // Fingerprint too thin — use product name as fallback
            query = sanitize(productName);
        }
        return query;
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    // ─────────────────────────────────────────────────────────────
    // PER-PLATFORM FETCHING
    // ─────────────────────────────────────────────────────────────

    private List<ProductCandidate> fetchFromPlatform(Platform platform, String query) {
        List<ProductCandidate> candidates = new ArrayList<>();
        try {
            String searchUrl = buildSearchUrl(platform, query);
            Document searchDoc = fetchDocument(searchUrl);
            if (searchDoc == null) {
                log.debug("[SCRAPER] Failed to fetch search page for {} — skipping", platform);
                return candidates;
            }

            List<String> productLinks = extractProductLinks(platform, searchDoc, searchUrl);
            log.debug("[SCRAPER] {} product links found on {}", productLinks.size(), platform);

            for (String link : productLinks) {
                try {
                    Document productDoc = fetchDocument(link);
                    if (productDoc == null) continue;

                    String title = extractTitle(productDoc);
                    String image = imageExtractionService.extractImage(productDoc, link);

                    ProductCandidate candidate = ProductCandidate.builder()
                            .platform(platform)
                            .title(title != null ? title : "")
                            .url(link)
                            .imageUrl(image)
                            .build();

                    candidates.add(candidate);
                    log.debug("[SCRAPER] Candidate from {}: '{}' | image: {}",
                            platform, truncate(title), image != null ? "yes" : "no");

                } catch (Exception e) {
                    log.debug("[SCRAPER] Failed to process product link {}: {}", truncate(link), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[SCRAPER] Platform {} fetch failed: {}", platform, e.getMessage());
        }
        return candidates;
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH URL BUILDER
    // ─────────────────────────────────────────────────────────────

    private String buildSearchUrl(Platform platform, String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return switch (platform) {
            case MYNTRA  -> "https://www.myntra.com/" + query.toLowerCase().replace(" ", "-");
            case AMAZON  -> "https://www.amazon.in/s?k=" + encoded;
            case FLIPKART -> "https://www.flipkart.com/search?q=" + encoded;
            case MEESHO  -> "https://www.meesho.com/search?q=" + encoded;
        };
    }

    // ─────────────────────────────────────────────────────────────
    // PRODUCT LINK EXTRACTION (up to MAX_PRODUCTS_PER_PLATFORM)
    // ─────────────────────────────────────────────────────────────

    private List<String> extractProductLinks(Platform platform, Document doc, String searchUrl) {
        return switch (platform) {
            case AMAZON   -> extractAmazonLinks(doc, searchUrl);
            case MYNTRA   -> extractMyntraLinks(doc);
            case FLIPKART -> extractFlipkartLinks(doc, searchUrl);
            case MEESHO   -> extractMeeshoLinks(doc);
        };
    }

    private List<String> extractAmazonLinks(Document doc, String searchUrl) {
        List<String> links = new ArrayList<>();
        Elements productLinks = doc.select("div[data-component-type=s-search-result] h2 a.a-link-normal[href*=/dp/]");
        if (productLinks.isEmpty()) {
            productLinks = doc.select("a[href*=/dp/]");
        }
        for (Element a : productLinks) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = resolveHref(a, searchUrl);
            if (href != null && href.contains("/dp/")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    private List<String> extractMyntraLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements productLinks = doc.select("a[href*=/buy/], li.product-base a[href^=/]");
        if (productLinks.isEmpty()) {
            productLinks = doc.select("a[href^=/][href*=-]");
        }
        for (Element a : productLinks) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;
            if (href.startsWith("/")) href = "https://www.myntra.com" + href.split("\\?")[0];
            else href = href.split("\\?")[0];
            if (href.contains("myntra.com") && !href.contains("/search") && !href.contains("/cart")) {
                links.add(href);
            }
        }
        return links;
    }

    private List<String> extractFlipkartLinks(Document doc, String searchUrl) {
        List<String> links = new ArrayList<>();
        Elements productLinks = doc.select("a[href*=/p/]");
        for (Element a : productLinks) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = resolveHref(a, searchUrl);
            if (href != null && href.contains("/p/")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    private List<String> extractMeeshoLinks(Document doc) {
        List<String> links = new ArrayList<>();
        // Meesho is JS-heavy; Jsoup may not find much. Graceful fail-fast.
        Elements productLinks = doc.select("a[href*=/product/]");
        for (Element a : productLinks) {
            if (links.size() >= MAX_PRODUCTS_PER_PLATFORM) break;
            String href = a.attr("abs:href");
            if (href != null && !href.isBlank() && href.contains("meesho.com")) {
                links.add(href.split("\\?")[0].trim());
            }
        }
        return links;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
        } catch (Exception e) {
            log.debug("[SCRAPER] Fetch failed for {}: {}", truncate(url), e.getMessage());
            return null;
        }
    }

    private String extractTitle(Document doc) {
        // Try og:title first (cleanest)
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String content = ogTitle.attr("content");
            if (content != null && !content.isBlank()) return content.trim();
        }
        // Fallback to <title>
        String title = doc.title();
        if (title != null && !title.isBlank()) return title.trim();
        return null;
    }

    private String resolveHref(Element a, String baseUrl) {
        String href = a.attr("abs:href");
        if (href == null || href.isBlank()) href = a.attr("href");
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("http")) return href;
        try {
            URI base = URI.create(baseUrl);
            return base.getScheme() + "://" + base.getHost() + (href.startsWith("/") ? "" : "/") + href;
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null || s.length() < 60) return s;
        return s.substring(0, 60) + "...";
    }
}
