package com.trendzy.scraper.website;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.trendzy.scraper.util.RandomDelayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts raw product data from Shopify storefronts.
 *
 * <h3>Two extraction strategies</h3>
 * <ol>
 *   <li><strong>JSON API</strong> — every Shopify store exposes
 *       {@code /products.json?limit=50}. This is the cleanest, most
 *       reliable approach and is tried first.</li>
 *   <li><strong>DOM fallback</strong> — iterates product-grid cards
 *       on the {@code /collections/all} page when the JSON route is
 *       blocked or returns no products.</li>
 * </ol>
 *
 * <p>Returns a list of {@link RawProduct} objects that are later
 * validated and transformed by the orchestrator.
 */
@Component
@Slf4j
public class ShopifyParser {

    private static final String PRODUCTS_JSON_PATH = "/products.json?limit=50";
    private static final String COLLECTIONS_PATH   = "/collections/all";
    private static final int    API_TIMEOUT_MS     = 12_000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────────────────────

    /**
     * Extracts raw product data from a Shopify store.
     *
     * @param page       live Playwright {@link Page} (already has the store loaded)
     * @param storeRoot  base URL, e.g. {@code https://example.myshopify.com}
     * @return list of {@link RawProduct} — may be empty, never null
     */
    public List<RawProduct> extractProducts(Page page, String storeRoot, String resolvedUrl) {
        List<RawProduct> products = new ArrayList<>();

        // ── Strategy 1: JSON API ──────────────────────────────
        products = extractViaJsonApi(page, storeRoot);
        if (!products.isEmpty()) {
            log.info("[SHOPIFY] JSON API yielded {} products for {}", products.size(), storeRoot);
            return products;
        }

        log.debug("[SHOPIFY] JSON API empty — falling back to DOM scrape for {}", storeRoot);

        // ── Strategy 2: DOM current page ──────────────────────
        try {
            page.navigate(resolvedUrl);
            RandomDelayUtil.delay();
            products = extractViaDomCards(page, storeRoot);
            if (!products.isEmpty()) {
                log.info("[SHOPIFY] DOM scrape of {} yielded {} products", resolvedUrl, products.size());
                return products;
            }
        } catch (Exception e) {
            log.trace("[SHOPIFY] Failed to scrape {}: {}", resolvedUrl, e.getMessage());
        }

        // ── Strategy 3: DOM collections page ──────────────────
        String collectionsUrl = storeRoot.replaceAll("/+$", "") + COLLECTIONS_PATH;
        if (!resolvedUrl.equalsIgnoreCase(collectionsUrl)) {
            try {
                page.navigate(collectionsUrl);
                RandomDelayUtil.delay();
                products = extractViaDomCards(page, storeRoot);
            } catch (Exception e) {
                log.warn("[SHOPIFY] DOM fallback failed for {}: {}", storeRoot, e.getMessage());
            }
        }

        log.info("[SHOPIFY] DOM scrape yielded {} products for {}", products.size(), storeRoot);
        return products;
    }

    // ─────────────────────────────────────────────────────────────
    // STRATEGY 1 — /products.json
    // ─────────────────────────────────────────────────────────────

    private List<RawProduct> extractViaJsonApi(Page page, String storeRoot) {
        List<RawProduct> results = new ArrayList<>();
        String apiUrl = storeRoot.replaceAll("/+$", "") + PRODUCTS_JSON_PATH;

        try {
            page.navigate(apiUrl);
            RandomDelayUtil.shortDelay();

            String body = page.innerText("body");
            if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
                log.debug("[SHOPIFY] JSON API returned non-JSON content for {}", storeRoot);
                return results;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode productsNode = root.path("products");

            if (!productsNode.isArray()) {
                log.debug("[SHOPIFY] products field is not an array for {}", storeRoot);
                return results;
            }

            for (JsonNode product : productsNode) {
                try {
                    RawProduct rp = parseJsonProduct(product, storeRoot);
                    if (rp != null) results.add(rp);
                } catch (Exception e) {
                    log.trace("[SHOPIFY] Skipping product node: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.debug("[SHOPIFY] JSON API request failed for {}: {}", storeRoot, e.getMessage());
        }

        return results;
    }

    private RawProduct parseJsonProduct(JsonNode product, String storeRoot) {
        String title  = product.path("title").asText("").trim();
        String handle = product.path("handle").asText("").trim();

        if (title.isBlank() || handle.isBlank()) return null;

        // Price — from first variant
        Double mainPrice = null;
        Double discountedPrice = null;
        JsonNode variants = product.path("variants");
        if (variants.isArray() && variants.size() > 0) {
            JsonNode firstVariant = variants.get(0);
            String priceStr = firstVariant.path("price").asText("0");
            String compareAtPriceStr = firstVariant.path("compare_at_price").asText(null);
            
            double variantPrice = 0.0;
            try { variantPrice = Double.parseDouble(priceStr); } catch (NumberFormatException ignored) {}
            
            double compareAtPrice = 0.0;
            if (compareAtPriceStr != null && !compareAtPriceStr.isBlank() && !compareAtPriceStr.equals("null")) {
                try { compareAtPrice = Double.parseDouble(compareAtPriceStr); } catch (NumberFormatException ignored) {}
            }
            
            if (compareAtPrice > variantPrice) {
                mainPrice = compareAtPrice;
                discountedPrice = variantPrice;
            } else {
                mainPrice = variantPrice;
                discountedPrice = null;
            }
        }

        // Image
        String imageUrl = null;
        JsonNode images = product.path("images");
        if (images.isArray() && images.size() > 0) {
            imageUrl = images.get(0).path("src").asText(null);
        }

        String productUrl = storeRoot.replaceAll("/+$", "") + "/products/" + handle;

        // Phase 3D Pre-filter (move to ScraperOrchestratorService but basic check here)
        if (mainPrice != null && mainPrice > 0 && (mainPrice < 300 || mainPrice > 5999)) {
            log.trace("[SHOPIFY] Pre-filtering by price: {} ({})", title, mainPrice);
            // We allow it through if price is 0 (unknown) as per requirements
            // However, we can discard if price is clearly out of range here
            return null;
        }

        return RawProduct.builder()
                .productName(title)
                .mainPrice(mainPrice)
                .discountedPrice(discountedPrice)
                .imageUrl(imageUrl)
                .productUrl(productUrl)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // STRATEGY 2 — DOM /collections/all
    // ─────────────────────────────────────────────────────────────

    private List<RawProduct> extractViaDomCards(Page page, String storeRoot) {
        List<RawProduct> results = new ArrayList<>();

        // Shopify product card selectors (cover most themes)
        List<String> cardSelectors = List.of(
                ".product-card",
                ".product-item",
                ".grid-product",
                "li.grid__item",
                "div[class*='product']",
                "article[class*='product']"
        );

        List<ElementHandle> cards = new ArrayList<>();
        for (String sel : cardSelectors) {
            try {
                List<ElementHandle> found = page.querySelectorAll(sel);
                if (!found.isEmpty() && found.size() >= 2) {
                    cards = found;
                    log.debug("[SHOPIFY] DOM: using selector '{}' → {} cards", sel, found.size());
                    break;
                }
            } catch (Exception ignored) {}
        }

        for (ElementHandle card : cards) {
            try {
                RawProduct rp = parseDomCard(card, storeRoot);
                if (rp != null) results.add(rp);
            } catch (Exception e) {
                log.trace("[SHOPIFY] DOM card parse failed: {}", e.getMessage());
            }
        }

        return results;
    }

    private RawProduct parseDomCard(ElementHandle card, String storeRoot) {
        // Title
        String title = null;
        for (String sel : List.of("h2", "h3", "h4", ".product-card__title",
                ".product-item__title", ".grid-product__title", "a[href*='/products/']")) {
            try {
                ElementHandle el = card.querySelector(sel);
                if (el != null) {
                    title = el.innerText().trim();
                    if (!title.isBlank()) break;
                }
            } catch (Exception ignored) {}
        }
        if (title == null || title.isBlank()) return null;

        // Price extraction
        Double mainPrice = null;
        Double discountedPrice = null;
        
        // 1. Try to find explicit sale + regular prices
        try {
            ElementHandle regularEl = card.querySelector("s.price-item--regular, del .amount, .price__regular s");
            ElementHandle saleEl = card.querySelector("span.price-item--sale, ins .amount, .price__sale .price-item");
            
            if (regularEl != null && saleEl != null) {
                String regRaw = regularEl.innerText().replaceAll("[^0-9.]", "");
                String saleRaw = saleEl.innerText().replaceAll("[^0-9.]", "");
                if (!regRaw.isBlank()) mainPrice = Double.parseDouble(regRaw);
                if (!saleRaw.isBlank()) discountedPrice = Double.parseDouble(saleRaw);
            }
        } catch (Exception ignored) {}
        
        // 2. If no explicit sale price structure found, grab the generic exact price
        if (mainPrice == null && discountedPrice == null) {
            for (String sel : List.of(".price", ".money", "[class*='price']", "span[data-price]")) {
                try {
                    ElementHandle el = card.querySelector(sel);
                    if (el != null) {
                        String raw = el.innerText().replaceAll("[^0-9.]", "");
                        if (!raw.isBlank()) { 
                            mainPrice = Double.parseDouble(raw); 
                            break; 
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Pre-filter by price
        if (mainPrice != null && mainPrice > 0 && (mainPrice < 300 || mainPrice > 5999)) return null;

        // Image
        String imageUrl = null;
        try {
            ElementHandle img = card.querySelector("img");
            if (img != null) {
                imageUrl = img.getAttribute("src");
                if (imageUrl == null) imageUrl = img.getAttribute("data-src");
                if (imageUrl != null && imageUrl.startsWith("//")) imageUrl = "https:" + imageUrl;
            }
        } catch (Exception ignored) {}

        // Product URL
        String productUrl = null;
        try {
            ElementHandle a = card.querySelector("a[href*='/products/']");
            if (a != null) {
                String href = a.getAttribute("href");
                if (href != null) {
                    productUrl = href.startsWith("http") ? href
                            : storeRoot.replaceAll("/+$", "") + href;
                }
            }
        } catch (Exception ignored) {}

        return RawProduct.builder()
                .productName(title)
                .mainPrice(mainPrice)
                .discountedPrice(discountedPrice)
                .imageUrl(imageUrl)
                .productUrl(productUrl)
                .build();
    }
}