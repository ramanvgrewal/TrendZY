package com.trendzy.service;

import com.trendzy.dto.response.TrendResponse;
import com.trendzy.dto.response.TrendStatsResponse;
import com.trendzy.exception.TrendZyException;
import com.trendzy.model.mongo.Product;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.mongo.ProductRepository;
import com.trendzy.repository.mongo.SignalRepository;
import com.trendzy.repository.mongo.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendService {

    private final TrendRepository trendRepository;
    private final ProductRepository productRepository;
    private final SignalRepository signalRepository;
    private final TokenBudgetService tokenBudgetService;

    @Value("${reddit.subreddits}")
    private String subredditsRaw;

    // ─────────────────────────────────────────────────────────────
    // GET PAGINATED TRENDS
    // ─────────────────────────────────────────────────────────────

    public Page<TrendResponse> getTrends(String tier, String vibeTag, Pageable pageable) {
        boolean hasTier = tier != null && !tier.isBlank();
        boolean hasVibe = vibeTag != null && !vibeTag.isBlank()
                && !vibeTag.equalsIgnoreCase("All");

        log.info("[TREND] getTrends — tier: {}, vibe: {}, page: {}",
                hasTier ? tier : "ALL",
                hasVibe ? vibeTag : "ALL",
                pageable.getPageNumber());

        Page<Trend> trends;

        if (hasTier && hasVibe) {
            trends = trendRepository
                    .findByTierAndVibeTagAndActiveTrue(tier, vibeTag, pageable);
        } else if (hasTier) {
            trends = trendRepository.findByTierAndActiveTrue(tier, pageable);
        } else if (hasVibe) {
            trends = trendRepository.findByVibeTagAndActiveTrue(vibeTag, pageable);
        } else {
            // findAll with active filter — never return inactive
            trends = trendRepository.findByActiveTrue(pageable);
        }

        log.info("[TREND] Found {} trends (total: {})",
                trends.getNumberOfElements(), trends.getTotalElements());

        // ── Fix N+1: fetch all products for this page in one query ──
        List<String> trendIds = trends.getContent().stream()
                .map(Trend::getId)
                .collect(Collectors.toList());

        Map<String, Product> productMap = productRepository
                .findByTrendIdIn(trendIds)
                .stream()
                .collect(Collectors.toMap(Product::getTrendId, p -> p,
                        (a, b) -> a)); // keep first if duplicate

        return trends.map(trend -> mapToResponse(trend, productMap.get(trend.getId())));
    }

    // ─────────────────────────────────────────────────────────────
    // GET SINGLE TREND BY ID
    // ─────────────────────────────────────────────────────────────

    public TrendResponse getTrendById(String id) {
        log.info("[TREND] getTrendById: {}", id);

        Trend trend = trendRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[TREND] Trend not found: {}", id);
                    return new TrendZyException("Trend not found", HttpStatus.NOT_FOUND);
                });

        Product product = productRepository.findByTrendId(id).orElse(null);
        log.debug("[TREND] Product enrichment found: {}", product != null);

        return mapToResponse(trend, product);
    }

    // ─────────────────────────────────────────────────────────────
    // GET TOP TREND (hero)
    // ─────────────────────────────────────────────────────────────

    public TrendResponse getTopTrend() {
        log.info("[TREND] Fetching top trend");

        List<Trend> topTrends = trendRepository.findTopTrends(Pageable.ofSize(1));
        if (topTrends.isEmpty()) {
            log.warn("[TREND] No trends found for hero");
            throw new TrendZyException("No trends found", HttpStatus.NOT_FOUND);
        }

        Trend top = topTrends.get(0);
        Product product = productRepository.findByTrendId(top.getId()).orElse(null);

        log.info("[TREND] Top trend: '{}' | score: {}", top.getProductName(), top.getTrendScore());
        return mapToResponse(top, product);
    }

    // ─────────────────────────────────────────────────────────────
    // GET RELATED TRENDS
    // ─────────────────────────────────────────────────────────────

    public List<TrendResponse> getRelatedTrends(String id, int size) {
        log.info("[TREND] Getting {} related trends for id: {}", size, id);

        Trend trend = trendRepository.findById(id)
                .orElseThrow(() -> new TrendZyException("Trend not found", HttpStatus.NOT_FOUND));

        List<Trend> related = trendRepository.findByCategoryAndIdNotAndActiveTrue(
                trend.getCategory(), trend.getId(), Pageable.ofSize(size));

        log.info("[TREND] Found {} related trends in category '{}'",
                related.size(), trend.getCategory());

        // Batch fetch products for related trends
        List<String> relatedIds = related.stream()
                .map(Trend::getId)
                .collect(Collectors.toList());

        Map<String, Product> productMap = productRepository
                .findByTrendIdIn(relatedIds)
                .stream()
                .collect(Collectors.toMap(Product::getTrendId, p -> p,
                        (a, b) -> a));

        return related.stream()
                .map(t -> mapToResponse(t, productMap.get(t.getId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // GET TICKER TRENDS
    // ─────────────────────────────────────────────────────────────

    public List<TrendResponse> getTickerTrends() {
        log.info("[TREND] Fetching ticker trends");

        List<Trend> trends = trendRepository.findTopTrends(Pageable.ofSize(10));

        List<String> ids = trends.stream()
                .map(Trend::getId)
                .collect(Collectors.toList());

        Map<String, Product> productMap = productRepository
                .findByTrendIdIn(ids)
                .stream()
                .collect(Collectors.toMap(Product::getTrendId, p -> p,
                        (a, b) -> a));

        return trends.stream()
                .map(t -> mapToResponse(t, productMap.get(t.getId())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // GET STATS
    // ─────────────────────────────────────────────────────────────

    public TrendStatsResponse getStats() {
        log.info("[TREND] Fetching trend stats");

        // Count subreddits from config — not hardcoded
        int subredditCount = (int) java.util.Arrays.stream(subredditsRaw.split(","))
                .filter(s -> !s.isBlank())
                .count();

        long signalsToday = signalRepository.countByCollectedAtAfter(
                LocalDateTime.now().toLocalDate().atStartOfDay());

        long trendingCount = trendRepository.countByTier("trending");
        long risingCount   = trendRepository.countByTier("rising");

        var tokenUsage = tokenBudgetService.getTodayUsage();

        log.info("[TREND] Stats — signalsToday: {}, trending: {}, rising: {}, subreddits: {}",
                signalsToday, trendingCount, risingCount, subredditCount);

        return TrendStatsResponse.builder()
                .signalsToday(signalsToday)
                .trendingCount(trendingCount)
                .risingCount(risingCount)
                .subredditsMonitored(subredditCount)
                .lastCollectionTime(LocalDateTime.now())
                .groqTokensUsedToday(tokenUsage.getTokensUsed())
                .groqTokensRemaining(tokenUsage.getTokensRemaining())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // MAPPER — accepts pre-fetched Product to avoid N+1
    // ─────────────────────────────────────────────────────────────

    private TrendResponse mapToResponse(Trend trend, Product product) {
        // Prefer enriched product data, fall back to trend data
        String imageUrl      = product != null && product.getPrimaryImageUrl() != null
                ? product.getPrimaryImageUrl()
                : trend.getImageUrl();
        String shopUrl       = product != null && product.getShopUrl() != null
                ? product.getShopUrl()
                : trend.getShopUrl();
        String amazonUrl     = product != null && product.getAmazonUrl() != null
                ? product.getAmazonUrl()
                : trend.getAmazonUrl();
        String myntraUrl     = product != null && product.getMyntraUrl() != null
                ? product.getMyntraUrl()
                : trend.getMyntraUrl();
        Double price         = product != null && product.getPrice() != null
                ? product.getPrice()
                : trend.getEstimatedPrice();
        Double originalPrice = product != null && product.getOriginalPrice() != null
                ? product.getOriginalPrice()
                : null;
        String platform      = product != null && product.getPlatform() != null
                ? product.getPlatform()
                : trend.getPlatform();
        String enrichmentStatus = trend.getEnrichmentStatus() != null
                ? trend.getEnrichmentStatus()
                : "PENDING";

        return TrendResponse.builder()
                .id(trend.getId())
                .productName(trend.getProductName())
                .category(trend.getCategory())
                .subcategory(trend.getSubcategory())
                .trendScore(trend.getTrendScore())
                .velocity(trend.getVelocity())
                .velocityLabel(trend.getVelocityLabel())
                .tier(trend.getTier())
                .vibeTags(trend.getVibeTags())
                .aiSummary(trend.getAiSummary())
                .whyTrending(trend.getWhyTrending())
                .indiaRelevanceNote(trend.getIndiaRelevanceNote())
                .totalSignals(trend.getTotalSignals())
                .detectedSubreddits(trend.getDetectedSubreddits())
                .youtubeVideoCount(trend.getYoutubeVideoCount())
                .images(product != null ? product.getImages() : null)
                .imageUrl(imageUrl)
                .primaryImageUrl(imageUrl)
                .shopUrl(shopUrl)
                .amazonUrl(amazonUrl)
                .myntraUrl(myntraUrl)
                .flipkartUrl(trend.getFlipkartUrl())
                .price(price)
                .estimatedPrice(trend.getEstimatedPrice())
                .originalPrice(originalPrice)
                .enrichmentStatus(enrichmentStatus)
                .platform(platform)
                .firstDetectedAt(trend.getFirstDetectedAt())
                .detectedAt(trend.getFirstDetectedAt())
                .build();
    }
}