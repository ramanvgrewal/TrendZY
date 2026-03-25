package com.trendzy.service;

import com.trendzy.dto.response.ProductCandidate;
import com.trendzy.model.mongo.Product;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.mongo.ProductRepository;
import com.trendzy.repository.mongo.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates batch product enrichment for trends.
 * Delegates actual resolution to ProductResolverService.
 * Only handles the batch loop, DB persistence, and status tracking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEnrichmentService {

    private final TrendRepository trendRepository;
    private final ProductRepository productRepository;
    private final ProductResolverService productResolverService;

    public int enrichProductsBatch() {
        List<Trend> unenriched = trendRepository.findPendingEnrichment();

        if (unenriched.isEmpty()) {
            log.info("[ENRICH] No unenriched trends found");
            return 0;
        }

        log.info("[ENRICH] Found {} unenriched trends", unenriched.size());
        int enrichedCount = 0;

        for (Trend trend : unenriched) {
            try {
                // Skip if already has a product
                if (productRepository.findByTrendId(trend.getId()).isPresent()) {
                    continue;
                }

                enrichTrend(trend);
                enrichedCount++;

            } catch (Exception e) {
                log.error("[ENRICH] Failed for '{}': {}", trend.getProductName(), e.getMessage());
            }
        }

        log.info("[ENRICH] Completed — {} enriched", enrichedCount);
        return enrichedCount;
    }

    /**
     * Enrich a single trend using the ProductResolverService pipeline.
     */
    private void enrichTrend(Trend trend) {
        log.info("[ENRICH] ─────────────────────────────────────");
        log.info("[ENRICH] Enriching '{}'", trend.getProductName());

        // ── Resolve best product via scoring pipeline ──
        ProductCandidate winner = productResolverService.resolve(trend);

        if (winner == null) {
            // No valid product found — mark trend accordingly
            trend.setEnrichmentStatus("NO_VALID_PRODUCT");
            trend.setLastUpdatedAt(LocalDateTime.now());
            trendRepository.save(trend);
            log.warn("[ENRICH] No valid product for '{}' — marked as NO_VALID_PRODUCT",
                    trend.getProductName());
            return;
        }

        // ── Determine platform-specific URLs from the winner ──
        String winnerUrl = winner.getUrl();
        String platform = winner.getPlatform() != null
                ? winner.getPlatform().name().toLowerCase() : "unknown";

        String amazonUrl = null, myntraUrl = null, flipkartUrl = null, meeshoUrl = null;
        switch (platform) {
            case "amazon"   -> amazonUrl = winnerUrl;
            case "myntra"   -> myntraUrl = winnerUrl;
            case "flipkart" -> flipkartUrl = winnerUrl;
            case "meesho"   -> meeshoUrl = winnerUrl;
        }

        // ── Build and save Product document ──
        Product product = Product.builder()
                .trendId(trend.getId())
                .productName(trend.getProductName())
                .primaryImageUrl(winner.getImageUrl())
                .images(winner.getImageUrl() != null ? List.of(winner.getImageUrl()) : List.of())
                .shopUrl(winnerUrl)
                .platform(platform)
                .matchScore(winner.getScore())
                .amazonUrl(amazonUrl)
                .myntraUrl(myntraUrl)
                .flipkartUrl(flipkartUrl)
                .meeshoUrl(meeshoUrl)
                .price(winner.getPrice())
                .originalPrice(null)
                .discount(null)
                .sizes(List.of())
                .colors(List.of())
                .description(trend.getAiSummary())
                .enrichedAt(LocalDateTime.now())
                .enrichmentStatus("COMPLETED")
                .build();

        productRepository.save(product);

        // ── Update trend with enrichment results ──
        trend.setImageUrl(winner.getImageUrl());
        trend.setShopUrl(winnerUrl);
        trend.setPlatform(platform);
        trend.setEnrichmentStatus("COMPLETED");
        // Legacy fields — populated for backward compat
        if (amazonUrl != null) trend.setAmazonUrl(amazonUrl);
        if (myntraUrl != null) trend.setMyntraUrl(myntraUrl);
        if (flipkartUrl != null) trend.setFlipkartUrl(flipkartUrl);
        trend.setLastUpdatedAt(LocalDateTime.now());
        trendRepository.save(trend);

        log.info("[ENRICH] ✅ '{}' enriched | platform: {} | score: {} | image: {}",
                trend.getProductName(), platform, winner.getScore(),
                winner.getImageUrl() != null ? "set" : "none");
    }
}
