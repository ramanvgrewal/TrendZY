package com.trendzy.service;

import com.trendzy.dto.response.ProductCandidate;
import com.trendzy.dto.response.ProductCandidate.Platform;
import com.trendzy.model.mongo.ProductFingerprint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Scores product candidates against the ProductFingerprint.
 *
 * Scoring weights (0–100 scale):
 *   Platform priority:    +15 / +10 / +5 / +2
 *   Brand match:          +25
 *   Product type match:   +20
 *   Color match:          +10
 *   Keyword matches:      +5 each (max +20)
 *
 * Penalties:
 *   "combo" / "pack of":  −30
 *   Category mismatch:    −20
 *   Brand mismatch:       REJECT
 */
@Service
@Slf4j
public class ProductScoringService {

    private static final double THRESHOLD_STRICT  = 60.0;
    private static final double THRESHOLD_RELAXED = 40.0;

    private static final Set<String> SPAM_TERMS = Set.of(
            "combo", "pack of", "set of", "bundle", "wholesale",
            "bulk", "lot of", "dozen", "refurbished", "used"
    );

    private static final Set<String> FASHION_TERMS = Set.of(
            "shirt", "t-shirt", "tshirt", "hoodie", "jacket", "jeans", "pants",
            "trouser", "dress", "skirt", "top", "sneaker", "shoe", "boot",
            "sandal", "slipper", "watch", "earbuds", "headphone", "bag",
            "tote", "backpack", "sunglasses", "cap", "hat", "belt",
            "kurta", "saree", "lehenga", "dupatta", "crop top", "jogger",
            "shorts", "blazer", "cardigan", "sweater", "sweatshirt",
            "lip", "lipstick", "foundation", "serum", "cream", "moisturizer",
            "sunscreen", "perfume", "fragrance", "eyeliner", "mascara",
            "blush", "concealer", "primer", "skincare", "bodycare"
    );

    private static final Pattern KNOWN_BRANDS_PATTERN = Pattern.compile(
            "\\b(nike|adidas|puma|reebok|new balance|converse|vans|fila|zara|h&m|" +
            "uniqlo|mango|forever 21|levi|wrangler|us polo|tommy hilfiger|" +
            "ralph lauren|calvin klein|gucci|louis vuitton|chanel|dior|" +
            "mac|maybelline|lakme|nykaa|minimalist|cetaphil|neutrogena|" +
            "the ordinary|innisfree|boat|jbl|sony|apple|samsung|casio|" +
            "fossil|titan|noise|fire-boltt|realme|oneplus|mi|xiaomi)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // ─────────────────────────────────────────────────────────────
    // SCORE ALL CANDIDATES
    // ─────────────────────────────────────────────────────────────

    public void scoreAll(List<ProductCandidate> candidates, ProductFingerprint fingerprint) {
        for (ProductCandidate c : candidates) {
            double score = computeScore(c, fingerprint);
            c.setScore(score);

            // Reject if below strict threshold
            if (score < THRESHOLD_STRICT) {
                c.setRejected(true);
                c.setRejectionReason("Score " + score + " below threshold " + THRESHOLD_STRICT);
            }

            log.debug("[SCORING] {} | '{}' → score: {} | rejected: {}",
                    c.getPlatform(), truncate(c.getTitle()), score, c.isRejected());
        }
    }

    /**
     * Re-score with relaxed threshold (used in fallback).
     */
    public void rescoreRelaxed(List<ProductCandidate> candidates) {
        for (ProductCandidate c : candidates) {
            if (c.isRejected() && c.getScore() >= THRESHOLD_RELAXED) {
                c.setRejected(false);
                c.setRejectionReason(null);
                log.debug("[SCORING] Relaxed: un-rejected '{}' with score {}",
                        truncate(c.getTitle()), c.getScore());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // COMPUTE INDIVIDUAL SCORE
    // ─────────────────────────────────────────────────────────────

    private double computeScore(ProductCandidate candidate, ProductFingerprint fp) {
        if (candidate.getTitle() == null || candidate.getTitle().isBlank()) {
            candidate.setRejected(true);
            candidate.setRejectionReason("Empty title");
            return 0;
        }

        String titleLower = candidate.getTitle().toLowerCase();
        double score = 0;

        // ── 1. Platform weight ──
        score += platformWeight(candidate.getPlatform());

        // ── 2. Brand match (+25) ──
        if (fp != null && fp.getBrand() != null && !fp.getBrand().isBlank()) {
            String brandLower = fp.getBrand().toLowerCase();
            if (titleLower.contains(brandLower)) {
                score += 25;
            } else {
                // Check if title contains a DIFFERENT known brand → hard reject
                if (KNOWN_BRANDS_PATTERN.matcher(titleLower).find()
                        && KNOWN_BRANDS_PATTERN.matcher(brandLower).find()) {
                    candidate.setRejected(true);
                    candidate.setRejectionReason("Brand mismatch: expected '" + fp.getBrand() + "'");
                    return 0;
                }
                // No brand match but not necessarily wrong — slight penalty
                score -= 5;
            }
        }

        // ── 3. Product type match (+20) ──
        if (fp != null && fp.getProductType() != null && !fp.getProductType().isBlank()) {
            String typeLower = fp.getProductType().toLowerCase();
            if (titleLower.contains(typeLower)) {
                score += 20;
            } else {
                // Try partial match (e.g., "sneakers" matching "sneaker")
                String typeRoot = typeLower.replaceAll("s$", "");
                if (titleLower.contains(typeRoot)) {
                    score += 15;
                }
            }
        }

        // ── 4. Color match (+10) ──
        if (fp != null && fp.getColor() != null && !fp.getColor().isBlank()) {
            if (titleLower.contains(fp.getColor().toLowerCase())) {
                score += 10;
            }
        }

        // ── 5. Keyword matches (+5 each, max +20) ──
        if (fp != null && fp.getKeywords() != null) {
            int kwScore = 0;
            for (String kw : fp.getKeywords()) {
                if (kw != null && !kw.isBlank() && titleLower.contains(kw.toLowerCase())) {
                    kwScore += 5;
                }
            }
            score += Math.min(kwScore, 20);
        }

        // ── 6. Spam penalties ──
        for (String spam : SPAM_TERMS) {
            if (titleLower.contains(spam)) {
                score -= 30;
                candidate.setRejected(true);
                candidate.setRejectionReason("Contains spam term: '" + spam + "'");
                break;
            }
        }

        // ── 7. Fashion relevance check ──
        boolean hasFashionTerm = FASHION_TERMS.stream()
                .anyMatch(titleLower::contains);
        if (!hasFashionTerm) {
            score -= 20;
        }

        // ── 8. Image availability bonus ──
        if (candidate.getImageUrl() != null && !candidate.getImageUrl().isBlank()) {
            score += 10;
        }

        return Math.max(score, 0);
    }

    private double platformWeight(Platform platform) {
        if (platform == null) return 0;
        return switch (platform) {
            case MYNTRA   -> 15;
            case AMAZON   -> 10;
            case FLIPKART -> 5;
            case MEESHO   -> 2;
        };
    }

    private static String truncate(String s) {
        if (s == null || s.length() < 50) return s;
        return s.substring(0, 50) + "...";
    }
}
