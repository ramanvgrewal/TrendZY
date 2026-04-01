package com.trendzy.scraper.website;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.trendzy.scraper.util.RandomDelayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback product extractor for non-Shopify brand websites.
 *
 * <h3>Extraction approach</h3>
 * <ol>
 *   <li>Try to find common product-grid / product-list containers by semantic
 *       selectors that appear across WooCommerce, custom React stores, etc.</li>
 *   <li>For each candidate card, extract: title, price, image, and link.</li>
 *   <li>If no grid containers are found, attempt a "shop", "products", or
 *       "collections" path enumeration.</li>
 * </ol>
 *
 * <p>This parser has lower fidelity than {@link ShopifyParser} — results should
 * always be validated before use.
 */
@Component
@Slf4j
public class GenericParser {

    /** Common sub-paths that lead to product listing pages. */
    private static final List<String> SHOP_PATHS = List.of(
            "/shop", "/products", "/collections/all", "/store",
            "/clothing", "/apparel", "/new-arrivals", "/all", "/collections"
    );

    /** Selectors for product-card containers (ordered by specificity). */
    private static final List<String> CARD_SELECTORS = List.of(
            ".product",
            ".product-card",
            ".product-item",
            ".woocommerce-loop-product__link",
            "li.type-product",
            "article.post-type-product",
            "div[class*='product']",
            "li[class*='product']",
            "[data-product-id]",
            "[data-product]"
    );

    private static final Pattern PRICE_PATTERN = Pattern.compile("[₹$]?\\s*(\\d[\\d,]*)(?:\\.\\d{1,2})?");

    // ─────────────────────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Tries to extract products from a generic brand website.
     *
     * @param page      a Playwright {@link Page} with the site already loaded
     * @param siteRoot  base URL, e.g. {@code https://mybrand.in}
     * @return list of {@link RawProduct}; empty list on failure
     */
    public List<RawProduct> extractProducts(Page page, String siteRoot) {
        List<RawProduct> products = new ArrayList<>();

        // ── Try the current page first ────────────────────────
        products = scrapeCurrentPage(page, siteRoot);
        if (!products.isEmpty()) {
            log.info("[GENERIC] {} products found on current page of {}", products.size(), siteRoot);
            return products;
        }

        // ── Enumerate candidate shop paths ────────────────────
        String base = siteRoot.replaceAll("/+$", "");
        for (String path : SHOP_PATHS) {
            if (!products.isEmpty()) break;
            String candidate = base + path;
            try {
                log.debug("[GENERIC] Trying path: {}", candidate);
                page.navigate(candidate);
                RandomDelayUtil.delay();
                products = scrapeCurrentPage(page, siteRoot);
            } catch (Exception e) {
                log.trace("[GENERIC] Path {} failed: {}", candidate, e.getMessage());
            }
        }

        log.info("[GENERIC] Total products extracted from {}: {}", siteRoot, products.size());
        return products;
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────

    private List<RawProduct> scrapeCurrentPage(Page page, String siteRoot) {
        List<RawProduct> results = new ArrayList<>();

        List<ElementHandle> cards = findProductCards(page);
        if (cards.isEmpty()) {
            log.trace("[GENERIC] No product cards on {}", page.url());
            return results;
        }

        log.debug("[GENERIC] {} candidate cards found on {}", cards.size(), page.url());

        for (ElementHandle card : cards) {
            try {
                RawProduct rp = parseCard(card, siteRoot);
                if (rp != null) results.add(rp);
            } catch (Exception e) {
                log.trace("[GENERIC] Card parse failed: {}", e.getMessage());
            }
        }
        return results;
    }

    private List<ElementHandle> findProductCards(Page page) {
        for (String sel : CARD_SELECTORS) {
            try {
                List<ElementHandle> found = page.querySelectorAll(sel);
                if (found.size() > 1) {
                    // At least 2 items → looks like a product grid
                    return found;
                }
            } catch (Exception ignored) {}
        }
        return List.of();
    }

    private RawProduct parseCard(ElementHandle card, String siteRoot) {
        // ── Title ─────────────────────────────────────────────
        String title = null;
        for (String sel : List.of("h2", "h3", "h4", ".product-title",
                ".woocommerce-loop-product__title", "[class*='name']", "a[title]")) {
            try {
                ElementHandle el = card.querySelector(sel);
                if (el != null) {
                    title = el.innerText().trim();
                    if (!title.isBlank()) break;
                }
            } catch (Exception ignored) {}
        }
        if (title == null || title.isBlank()) return null;

        // ── Price ─────────────────────────────────────────────
        double price = 0.0;
        for (String sel : List.of(".price", ".amount", ".woocommerce-Price-amount",
                "[class*='price']", "[itemprop='price']", "[class*='amount']")) {
            try {
                ElementHandle el = card.querySelector(sel);
                if (el != null) {
                    String raw = el.innerText();
                    Matcher m = PRICE_PATTERN.matcher(raw);
                    if (m.find()) {
                        price = Double.parseDouble(m.group(1).replace(",", ""));
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Pre-filter by price (Phase 3D)
        if (price > 0 && (price < 400 || price > 2000)) return null;

        // ── Image ─────────────────────────────────────────────
        String imageUrl = null;
        try {
            ElementHandle img = card.querySelector("img");
            if (img != null) {
                imageUrl = img.getAttribute("src");
                if (imageUrl == null) imageUrl = img.getAttribute("data-src");
                if (imageUrl == null) imageUrl = img.getAttribute("data-lazy-src");
                if (imageUrl != null && imageUrl.startsWith("//")) imageUrl = "https:" + imageUrl;
                // Skip data URIs and tiny placeholder images
                if (imageUrl != null && (imageUrl.startsWith("data:") || imageUrl.length() < 20)) {
                    imageUrl = null;
                }
            }
        } catch (Exception ignored) {}

        // ── Product URL ───────────────────────────────────────
        String productUrl = null;
        try {
            ElementHandle a = card.querySelector("a[href]");
            if (a != null) {
                String href = a.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    productUrl = href.startsWith("http") ? href
                            : siteRoot.replaceAll("/+$", "") + (href.startsWith("/") ? "" : "/") + href;
                }
            }
        } catch (Exception ignored) {}

        return RawProduct.builder()
                .productName(title)
                .price(price)
                .imageUrl(imageUrl)
                .productUrl(productUrl)
                .build();
    }
}