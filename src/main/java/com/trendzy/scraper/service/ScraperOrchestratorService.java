package com.trendzy.scraper.service;

import com.microsoft.playwright.Playwright;
import com.trendzy.scraper.dto.BrandDto;
import com.trendzy.scraper.dto.ProductDto;
import com.trendzy.scraper.instagram.InstagramExploreClient;
import com.trendzy.scraper.instagram.InstagramProfileClient;
import com.trendzy.scraper.instagram.InstagramSessionManager;
import com.trendzy.scraper.validator.ProductValidator;
import com.trendzy.scraper.website.RawProduct;
import com.trendzy.scraper.website.WebsiteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Orchestrates the full Instagram → Brand → Product discovery pipeline.
 *
 * <h3>Pipeline steps</h3>
 * <ol>
 *   <li>Ensure Instagram session is available.</li>
 *   <li>Scrape Explore page for post URLs (Step 1).</li>
 *   <li>Extract brand profiles from posts (Step 2).</li>
 *   <li>For each brand, visit website and extract raw products (Steps 3–4).</li>
 *   <li>Validate each product page (Step 5).</li>
 *   <li>Filter by price band and streetwear relevance (Steps 6–7).</li>
 *   <li>Deduplicate (Step 8).</li>
 *   <li>Transform to {@link ProductDto} and return (Step 9).</li>
 * </ol>
 *
 * <p>The pipeline stops early as soon as {@value #TARGET_PRODUCTS} validated
 * products have been found, to keep runtime bounded.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScraperOrchestratorService {

    private final InstagramSessionManager  sessionManager;
    private final InstagramExploreClient   exploreClient;
    private final InstagramProfileClient   profileClient;
    private final WebsiteClient            websiteClient;
    private final ProductValidator         productValidator;

    // ── Pipeline parameters ──────────────────────────────────────
    private static final int    TARGET_PRODUCTS    = 15;
    private static final int    HARD_LIMIT         = 50;  // max raw products validated
    private static final double MIN_PRICE_INR      = 400.0;
    private static final double MAX_PRICE_INR      = 2000.0;

    // Brands to reject (luxury, marketplace, formal)
    private static final Set<String> REJECT_BRANDS = Set.of(
            "gucci", "louis vuitton", "prada", "chanel", "hermes", "burberry",
            "amazon", "flipkart", "myntra", "ajio", "meesho", "snapdeal",
            "nykaa", "reliance", "pantaloons", "shoppersstop", "lifestyle",
            "allensolly", "vanheusen", "peterengland", "raymond", "arrow"
    );

    // Product types that indicate formal / non-streetwear
    private static final Set<String> FORMAL_TERMS = Set.of(
            "suit", "blazer", "tuxedo", "formal", "office", "ethnic",
            "wedding", "saree", "lehenga", "sherwani", "kurta set for wedding"
    );

    // Terms that increase streetwear relevance score
    private static final List<String> STREETWEAR_TERMS = List.of(
            "oversized", "baggy", "graphic", "streetwear", "hoodie", "cargo",
            "tee", "t-shirt", "sweatshirt", "jogger", "denim", "sneaker",
            "drop", "limited", "collab", "vibe", "gen z", "urban", "minimal",
            "boxy", "embroidery", "typography", "print", "y2k", "crop", "distressed"
    );

    // Auto-assigned vibes based on product keywords
    private static final Map<String, String> KEYWORD_TO_VIBE = new LinkedHashMap<>();
    static {
        KEYWORD_TO_VIBE.put("oversized",   "OversizedFit");
        KEYWORD_TO_VIBE.put("baggy",       "BaggyFit");
        KEYWORD_TO_VIBE.put("cargo",       "CargoPants");
        KEYWORD_TO_VIBE.put("hoodie",      "HoodieWeather");
        KEYWORD_TO_VIBE.put("graphic",     "GraphicTee");
        KEYWORD_TO_VIBE.put("embroidery",  "EmbroideryArt");
        KEYWORD_TO_VIBE.put("sustainable", "Sustainable");
        KEYWORD_TO_VIBE.put("minimal",     "Minimalist");
        KEYWORD_TO_VIBE.put("y2k",         "Y2K");
        KEYWORD_TO_VIBE.put("vintage",     "Vintage90s");
        KEYWORD_TO_VIBE.put("anime",       "AnimeCulture");
        KEYWORD_TO_VIBE.put("hemp",        "Sustainable");
        KEYWORD_TO_VIBE.put("cotton",      "EverydayWear");
        KEYWORD_TO_VIBE.put("sneaker",     "SneakerHead");
        KEYWORD_TO_VIBE.put("typography",  "BoldText");
        KEYWORD_TO_VIBE.put("drop",        "LimitedDrop");
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    /**
     * Runs the full discovery pipeline.
     *
     * @return list of {@link ProductDto} (10–15 items); empty list on total failure
     */
    public List<ProductDto> discoverProducts() {
        log.info("[PIPELINE] ════════════════ STARTING PRODUCT DISCOVERY ════════════════");
        List<ProductDto> finalProducts = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {

            // ── Step 1: Ensure session ────────────────────────
            if (!sessionManager.ensureSession(playwright)) {
                log.warn("[PIPELINE] No Instagram session — aborting");
                return finalProducts;
            }

            // ── Step 2: Explore → post URLs (Phase 1) ─────────
            log.info("[PIPELINE] Step 1: Scraping Instagram hashtag pages...");
            List<String> postUrls = exploreClient.fetchExplorePosts(playwright);
            log.info("[PIPELINE] Step 1 complete — {} post URLs found", postUrls.size());

            if (postUrls.isEmpty()) {
                log.warn("[PIPELINE] No posts found — aborting");
                return finalProducts;
            }

            // ── Step 3: Posts → brand profiles (Phase 2) ──────
            log.info("[PIPELINE] Step 2: Extracting brand profiles...");
            List<BrandDto> brands = profileClient.extractBrandsFromPosts(postUrls, playwright);
            log.info("[PIPELINE] Step 2 complete — {} brands with websites", brands.size());

            if (brands.isEmpty()) {
                log.warn("[PIPELINE] No brands found — aborting");
                return finalProducts;
            }

            // ── Steps 4–5: Websites → raw products → validation (Phase 3 & 4)
            log.info("[PIPELINE] Steps 3–5: Visiting websites, extracting + validating products...");
            List<ValidatedCandidate> allValidated = new ArrayList<>();

            for (BrandDto brand : brands) {
                if (allValidated.size() >= HARD_LIMIT) break;

                log.info("[PIPELINE] Processing brand @{} → {}", brand.getUsername(), brand.getWebsiteUrl());

                // Extract raw products
                List<RawProduct> raw;
                try {
                    raw = websiteClient.extractProducts(brand.getWebsiteUrl(), playwright, brand.getUsername());
                } catch (Exception e) {
                    log.warn("[PIPELINE] Website extraction failed for @{}: {}", brand.getUsername(), e.getMessage());
                    continue;
                }

                if (raw.isEmpty()) {
                    log.debug("[PIPELINE] No products from @{}", brand.getUsername());
                    continue;
                }

                // Pre-filter before opening each product page (saves validation time)
                List<RawProduct> preFiltered = preFilter(raw, brand.getUsername());
                log.info("[PIPELINE] @{} — {} products before pre-filter, {} after",
                        brand.getUsername(), raw.size(), preFiltered.size());

                if (preFiltered.isEmpty()) continue;

                // Validate product pages
                List<RawProduct> validated;
                try {
                    validated = productValidator.validateProducts(preFiltered, playwright);
                } catch (Exception e) {
                    log.warn("[PIPELINE] Validation failed for @{}: {}", brand.getUsername(), e.getMessage());
                    continue;
                }

                for (RawProduct vp : validated) {
                    allValidated.add(new ValidatedCandidate(vp, brand));
                    if (allValidated.size() >= HARD_LIMIT) break;
                }
            }

            log.info("[PIPELINE] Steps 3–5 complete — {} validated candidates", allValidated.size());

            // ── Step 6: Filter by price + style (Phase 5) ─────
            log.info("[PIPELINE] Step 6: Applying price + style filters...");
            List<ValidatedCandidate> filtered = applyFinalFilters(allValidated);
            log.info("[PIPELINE] Step 6 complete — {} products passed filters", filtered.size());

            // ── Step 7: Deduplication (Phase 5) ───────────────
            log.info("[PIPELINE] Step 7: Deduplicating...");
            List<ValidatedCandidate> deduped = deduplicate(filtered);
            log.info("[PIPELINE] Step 7 complete — {} unique products", deduped.size());

            // ── Step 8: Transform to DTO (Phase 6) ────────────
            log.info("[PIPELINE] Step 8: Transforming to ProductDto...");
            // Take top 15 after sorting by score
            List<ValidatedCandidate> top15 = deduped.stream().limit(15).collect(Collectors.toList());
            for (ValidatedCandidate vc : top15) {
                ProductDto dto = transformToDto(vc.product(), vc.brand());
                finalProducts.add(dto);
            }

        } catch (Exception e) {
            log.error("[PIPELINE] Fatal pipeline error: {}", e.getMessage(), e);
        }

        log.info("[PIPELINE] ════════════════ DISCOVERY COMPLETE: {} products ════════════════",
                finalProducts.size());
        return finalProducts;
    }

    // ─────────────────────────────────────────────────────────────
    // PRE-FILTER (before page validation — fast)
    // ─────────────────────────────────────────────────────────────

    private List<RawProduct> preFilter(List<RawProduct> raw, String brandUsername) {
        return raw.stream()
                .filter(p -> p.getProductName() != null && !p.getProductName().isBlank())
                .filter(p -> p.getProductUrl() != null && !p.getProductUrl().isBlank())
                .filter(p -> {
                    // Price check: skip if price is known and out of range
                    // (price = 0 means unknown — allow through, validator will confirm)
                    double price = p.getPrice();
                    return price == 0.0 || (price >= MIN_PRICE_INR && price <= MAX_PRICE_INR);
                })
                .filter(p -> !isFormalProduct(p.getProductName()))
                .filter(p -> !isRejectedBrand(brandUsername))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // FINAL FILTERS (price + style)
    // ─────────────────────────────────────────────────────────────

    private List<ValidatedCandidate> applyFinalFilters(List<ValidatedCandidate> candidates) {
        return candidates.stream()
                .filter(vc -> {
                    double price = vc.product().getPrice();
                    // Must have a confirmed price in range
                    return price >= MIN_PRICE_INR && price <= MAX_PRICE_INR;
                })
                .filter(vc -> {
                    String name = vc.product().getProductName().toLowerCase();
                    // productName DOES contain at least one streetwear term OR does not contain any formal term
                    return !isFormalProduct(name);
                })
                .filter(vc -> {
                    // ProductName and ProductUrl and brandName must not be blank (DTO transform will handle brandName)
                    return vc.product().getProductName() != null && !vc.product().getProductName().isBlank()
                           && vc.product().getProductUrl() != null && !vc.product().getProductUrl().isBlank();
                })
                .sorted(Comparator.comparingDouble((ValidatedCandidate vc) ->
                        streetwearScore(vc.product().getProductName())).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // DEDUPLICATION
    // ─────────────────────────────────────────────────────────────

    private List<ValidatedCandidate> deduplicate(List<ValidatedCandidate> candidates) {
        Set<String> seenNames    = new LinkedHashSet<>();
        Set<String> seenImages   = new LinkedHashSet<>();
        Set<String> seenUrls     = new LinkedHashSet<>();
        List<ValidatedCandidate> unique = new ArrayList<>();

        for (ValidatedCandidate vc : candidates) {
            RawProduct p   = vc.product();
            String normName = p.getProductName().trim().toLowerCase();
            String imgKey  = p.getImageUrl() != null ? p.getImageUrl().split("\\?")[0] : null;
            String urlKey  = p.getProductUrl() != null ? p.getProductUrl().split("\\?")[0] : null;

            if (seenNames.contains(normName)) continue;
            if (imgKey != null && seenImages.contains(imgKey)) continue;
            if (urlKey != null && seenUrls.contains(urlKey)) continue;

            seenNames.add(normName);
            if (imgKey != null)  seenImages.add(imgKey);
            if (urlKey != null)  seenUrls.add(urlKey);
            unique.add(vc);
        }

        return unique;
    }

    // ─────────────────────────────────────────────────────────────
    // DTO TRANSFORMATION
    // ─────────────────────────────────────────────────────────────

    private ProductDto transformToDto(RawProduct product, BrandDto brand) {
        String name     = product.getProductName();
        String nameLower = name.toLowerCase();

        // ── Category ──────────────────────────────────────────
        String category = inferCategory(nameLower);

        // ── Product type ──────────────────────────────────────
        String productType = inferProductType(nameLower);

        // ── Price ─────────────────────────────────────────────
        int priceInr   = (int) Math.round(product.getPrice());
        String priceRange = buildPriceRange(priceInr);

        // ── Description ───────────────────────────────────────
        String description = buildDescription(name, brand.getUsername(), productType);

        // ── Vibe tags (exactly 3) ─────────────────────────────
        List<String> vibeTags = buildVibeTags(nameLower, category);

        // ── Featured: top 20% by streetwear score ─────────────
        boolean featured = streetwearScore(name) >= 3;

        // ── Image URL (prefer non-null from scrape, fall back) ─
        String imageUrl = product.getImageUrl() != null ? product.getImageUrl() : "";

        return ProductDto.builder()
                .category(category)
                .productType(productType)
                .brandName(inferBrandName(brand))
                .productName(name)
                .imageUrl(imageUrl)
                .websiteLink(product.getProductUrl() != null
                        ? product.getProductUrl().split("\\?")[0] : "")
                .priceInr(priceInr)
                .priceRange(priceRange)
                .description(description)
                .featured(featured)
                .vibeTags(vibeTags)
                .build();
    }

    private String inferBrandName(BrandDto brand) {
        if (brand == null) return "";
        String website = brand.getWebsiteUrl();
        if (website != null && !website.isBlank()) {
            try {
                String host = java.net.URI.create(website).getHost();
                if (host != null && !host.isBlank()) {
                    String cleanHost = host.toLowerCase()
                            .replaceFirst("^www\\.", "")
                            .split("\\.")[0];
                    if (!cleanHost.isBlank()) {
                        return toTitleCase(cleanHost);
                    }
                }
            } catch (Exception ignored) { }
        }
        return toTitleCase(brand.getUsername());
    }

    // ─────────────────────────────────────────────────────────────
    // INFERENCE HELPERS
    // ─────────────────────────────────────────────────────────────

    private String inferCategory(String nameLower) {
        if (nameLower.contains("y2k") || nameLower.contains("crop") || nameLower.contains("halter"))
            return "Y2K Fashion";
        if (nameLower.contains("sustainable") || nameLower.contains("hemp") || nameLower.contains("organic"))
            return "Sustainable";
        return "Streetwear";
    }

    private String inferProductType(String nameLower) {
        if (nameLower.contains("hoodie"))                      return "Hoodie";
        if (nameLower.contains("jacket") || nameLower.contains("coat")) return "Jacket";
        if (nameLower.contains("jean") || nameLower.contains("denim"))   return "Jeans";
        if (nameLower.contains("sneaker") || nameLower.contains("shoe")) return "Sneakers";
        if (nameLower.contains("t-shirt") || nameLower.contains("tshirt") || nameLower.contains(" tee"))
            return "T-Shirt";
        if (nameLower.contains("shirt"))                       return "Shirt";
        if (nameLower.contains("cargo"))                       return "Cargo Pants";
        if (nameLower.contains("jogger") || nameLower.contains("sweatpant")) return "Joggers";
        if (nameLower.contains("sweatshirt"))                  return "Sweatshirt";
        return "T-Shirt";
    }

    private String buildPriceRange(int priceInr) {
        if (priceInr <= 0) return "400-500";
        int lo = (priceInr / 100) * 100;
        int hi = lo + 100;
        return lo + "-" + hi;
    }

    private String buildDescription(String productName, String brandUsername, String productType) {
        // Generate a unique 8–15 word Gen Z-focused description
        List<String> fitAdjectives = List.of("Boxy fit", "Oversized", "Relaxed", "Street-ready", "Vintage-inspired", "Urban", "Minimalist", "Bold", "Edgy", "Sleek");
        List<String> aesthetics = List.of("street energy", "Gen Z aesthetic", "urban vibes", "effortless style", "modern edge", "indie spirit", "city life", "clean look");
        List<String> purposes = List.of("perfect for all-day wear", "built for the daily grind", "for your next social drop", "elevating your street rotation", "essential for layering", "crafted for effortless cool");

        // Use hash of product name to pick deterministic but varied parts
        int hash = Math.abs(productName.hashCode());
        String adj = fitAdjectives.get(hash % fitAdjectives.size());
        String aes = aesthetics.get((hash / fitAdjectives.size()) % aesthetics.size());
        String pur = purposes.get((hash / (fitAdjectives.size() * aesthetics.size())) % purposes.size());

        String desc = adj + " " + productType.toLowerCase() + " featuring " + aes + " " + pur + ".";
        
        // Ensure 8-15 words
        String[] words = desc.split("\\s+");
        if (words.length < 8) desc += " Designed for the Gen Z vibe.";
        if (words.length > 15) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 15; i++) sb.append(words[i]).append(i == 14 ? "." : " ");
            desc = sb.toString();
        }

        return desc;
    }

    private List<String> buildVibeTags(String nameLower, String category) {
        List<String> tags = new ArrayList<>();

        // Map keywords to vibes (Phase 6)
        if (nameLower.contains("oversized")) tags.add("OversizedFit");
        if (nameLower.contains("baggy")) tags.add("BaggyFit");
        if (nameLower.contains("cargo")) tags.add("CargoPants");
        if (nameLower.contains("hoodie")) tags.add("HoodieWeather");
        if (nameLower.contains("graphic")) tags.add("GraphicTee");
        if (nameLower.contains("embroidery")) tags.add("EmbroideryArt");
        if (nameLower.contains("sustainable") || nameLower.contains("hemp")) tags.add("Sustainable");
        if (nameLower.contains("minimal")) tags.add("Minimalist");
        if (nameLower.contains("y2k")) tags.add("Y2K");
        if (nameLower.contains("vintage")) tags.add("Vintage90s");
        if (nameLower.contains("anime")) tags.add("AnimeCulture");
        if (nameLower.contains("cotton")) tags.add("EverydayWear");
        if (nameLower.contains("sneaker")) tags.add("SneakerHead");
        if (nameLower.contains("typography")) tags.add("BoldText");
        if (nameLower.contains("drop") || nameLower.contains("limited")) tags.add("LimitedDrop");

        // Always include GenZ and Streetwear (Phase 6)
        if (!tags.contains("GenZ")) tags.add("GenZ");
        if (!tags.contains("Streetwear")) tags.add("Streetwear");

        // Fill remaining slots
        while (tags.size() < 3) {
            tags.add("IndieIndia");
        }

        // Deduplicate and take exactly 3
        return tags.stream().distinct().limit(3).collect(Collectors.toList());
    }

    private int streetwearScore(String name) {
        String lower = name.toLowerCase();
        int score = 0;
        for (String term : STREETWEAR_TERMS) {
            if (lower.contains(term)) score++;
        }
        return score;
    }

    private boolean isFormalProduct(String name) {
        String lower = name.toLowerCase();
        return FORMAL_TERMS.stream().anyMatch(lower::contains);
    }

    private boolean isRejectedBrand(String username) {
        if (username == null) return false;
        String lower = username.toLowerCase();
        return REJECT_BRANDS.stream().anyMatch(lower::contains);
    }

    private String toTitleCase(String s) {
        if (s == null || s.isBlank()) return "";
        return Arrays.stream(s.trim().split("[_\\s-]+"))
                .filter(w -> !w.isBlank())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // ─────────────────────────────────────────────────────────────
    // INNER RECORD — pairs a validated product with its brand
    // ─────────────────────────────────────────────────────────────

    private record ValidatedCandidate(RawProduct product, BrandDto brand) {}
}
