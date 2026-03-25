package com.trendzy.service;

import com.trendzy.dto.response.ProductCandidate;
import com.trendzy.model.mongo.ProductFingerprint;
import com.trendzy.model.mongo.Trend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the product resolution pipeline:
 *   1. Build/use ProductFingerprint from the trend
 *   2. Fetch candidates from all platforms via PlatformScraperService
 *   3. Score candidates via ProductScoringService
 *   4. Validate and select the TOP 1 best match
 *   5. Fallback: relax threshold if no strict match found
 *   6. Return null if no valid product (caller marks as NO_VALID_PRODUCT)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductResolverService {

    private final PlatformScraperService scraperService;
    private final ProductScoringService scoringService;

    /**
     * Resolve the best product for a given trend.
     * Returns the winning ProductCandidate, or null if none pass validation.
     */
    public ProductCandidate resolve(Trend trend) {
        log.info("[RESOLVER] ═══════════════════════════════════════════");
        log.info("[RESOLVER] Resolving product for trend: '{}'", trend.getProductName());

        // ── 1. Build fingerprint ──
        ProductFingerprint fp = trend.getFingerprint();
        if (fp == null) {
            // Build a minimal fingerprint from product name / category
            fp = ProductFingerprint.builder()
                    .brand("")
                    .productType(trend.getSubcategory() != null ? trend.getSubcategory() : "")
                    .color("")
                    .gender("unisex")
                    .keywords(List.of())
                    .build();
            log.warn("[RESOLVER] No fingerprint on trend — using minimal fallback");
        }

        log.info("[RESOLVER] Fingerprint: brand='{}', type='{}', color='{}', gender='{}', keywords={}",
                fp.getBrand(), fp.getProductType(), fp.getColor(), fp.getGender(), fp.getKeywords());

        // ── 2. Fetch candidates from all platforms ──
        List<ProductCandidate> candidates = scraperService.fetchAllCandidates(fp, trend.getProductName());

        if (candidates.isEmpty()) {
            log.warn("[RESOLVER] No candidates found for '{}' — marking as no valid product",
                    trend.getProductName());
            return null;
        }

        // ── 3. Score all candidates ──
        scoringService.scoreAll(candidates, fp);

        // ── 4. First pass: strict selection ──
        Optional<ProductCandidate> winner = selectBest(candidates);

        if (winner.isPresent()) {
            ProductCandidate best = winner.get();
            log.info("[RESOLVER] ✅ WINNER: {} | '{}' | score: {} | image: {}",
                    best.getPlatform(), truncate(best.getTitle()),
                    best.getScore(), best.getImageUrl() != null ? "yes" : "no");
            return best;
        }

        // ── 5. Fallback: relax threshold ──
        log.info("[RESOLVER] No strict match — retrying with relaxed threshold...");
        scoringService.rescoreRelaxed(candidates);

        winner = selectBest(candidates);

        if (winner.isPresent()) {
            ProductCandidate best = winner.get();
            log.info("[RESOLVER] ✅ RELAXED WINNER: {} | '{}' | score: {}",
                    best.getPlatform(), truncate(best.getTitle()), best.getScore());
            return best;
        }

        // ── 6. No valid product ──
        log.warn("[RESOLVER] ❌ No valid product found for '{}' after relaxed retry",
                trend.getProductName());

        // Log top 3 rejected candidates for debugging
        candidates.stream()
                .sorted(Comparator.comparingDouble(ProductCandidate::getScore).reversed())
                .limit(3)
                .forEach(c -> log.debug("[RESOLVER] Rejected candidate: {} | '{}' | score: {} | reason: {}",
                        c.getPlatform(), truncate(c.getTitle()), c.getScore(), c.getRejectionReason()));

        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // SELECTION — pick top-1 non-rejected candidate with image
    // ─────────────────────────────────────────────────────────────

    private Optional<ProductCandidate> selectBest(List<ProductCandidate> candidates) {
        return candidates.stream()
                .filter(c -> !c.isRejected())
                .filter(c -> c.getImageUrl() != null && !c.getImageUrl().isBlank())
                .filter(c -> c.getUrl() != null && !c.getUrl().isBlank())
                .max(Comparator.comparingDouble(ProductCandidate::getScore));
    }

    private static String truncate(String s) {
        if (s == null || s.length() < 60) return s;
        return s.substring(0, 60) + "...";
    }
}
