package com.trendzy.service;

import com.trendzy.dto.request.AffiliateClickRequest;
import com.trendzy.model.jpa.ClickEvent;
import com.trendzy.model.jpa.User;
import com.trendzy.model.mongo.CuratedProduct;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.jpa.ClickEventRepository;
import com.trendzy.repository.mongo.CuratedProductRepository;
import com.trendzy.repository.mongo.TrendRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AffiliateService {

    private final ClickEventRepository clickEventRepository;
    private final TrendRepository trendRepository;
    private final CuratedProductRepository curatedProductRepository;
    private final UserService userService;

    // ── Your actual registered affiliate tags ──────────────────
    private static final String AMAZON_TAG   = "trendpulse-21";
    private static final String MYNTRA_TAG   = "trendpulse";
    private static final String FLIPKART_TAG = "trendpulse";

    // ─────────────────────────────────────────────────────────────
    // GENERATE AFFILIATE LINK
    // ─────────────────────────────────────────────────────────────

    public String generateAffiliateLink(String productId, String platform) {
        log.info("[AFFILIATE] Generating link — productId: {}, platform: {}",
                productId, platform);

        if (productId == null || productId.isBlank()) {
            log.warn("[AFFILIATE] productId is null — returning fallback");
            return buildFallbackUrl(platform, "product");
        }

        if (platform == null || platform.isBlank()) {
            log.warn("[AFFILIATE] platform is null for product {} — defaulting to amazon",
                    productId);
            platform = "amazon";
        }

        String originalUrl = findProductUrl(productId, platform);

        if (originalUrl == null || originalUrl.isBlank()) {
            log.warn("[AFFILIATE] No URL found for product {} on {} — using fallback",
                    productId, platform);
            return buildFallbackUrl(platform, productId);
        }

        String affiliateUrl = appendAffiliateTag(originalUrl, platform);
        log.info("[AFFILIATE] ✅ Link generated for product {}: {}", productId, affiliateUrl);
        return affiliateUrl;
    }

    // ─────────────────────────────────────────────────────────────
    // TRACK CLICK
    // ─────────────────────────────────────────────────────────────

    public void trackClick(String productId,
                           String platform,
                           String source,
                           String productType,
                           HttpServletRequest request) {

        if (productId == null || productId.isBlank()) {
            log.warn("[AFFILIATE] trackClick — null productId, skipping");
            return;
        }
        if (platform == null || platform.isBlank()) {
            log.warn("[AFFILIATE] trackClick — null platform for product {}, skipping",
                    productId);
            return;
        }

        // Resolve userId safely
        Long userId = null;
        try {
            User user = userService.getCurrentUser();
            if (user != null) userId = user.getId();
        } catch (AuthenticationException e) {
            log.debug("[AFFILIATE] Click from unauthenticated user");
        } catch (Exception e) {
            log.warn("[AFFILIATE] Could not resolve user for click tracking: {}",
                    e.getMessage());
        }

        String ipAddress = extractClientIp(request);

        ClickEvent clickEvent = ClickEvent.builder()
                .productId(productId)
                .productType(productType != null ? productType : "trend")
                .platform(platform.toLowerCase())
                .source(source)
                .userId(userId)
                .ipAddress(ipAddress)
                .clickedAt(LocalDateTime.now())
                .build();

        clickEventRepository.save(clickEvent);

        log.info("[AFFILIATE] ✅ Click tracked — product: {}, platform: {}, source: {}, user: {}",
                productId,
                platform,
                source,
                userId != null ? userId : "anonymous");
    }

    // ─────────────────────────────────────────────────────────────
    // FIND PRODUCT URL FROM DB
    // ─────────────────────────────────────────────────────────────

    private String findProductUrl(String productId, String platform) {
        // Check trends collection first
        Optional<Trend> trend = trendRepository.findById(productId);
        if (trend.isPresent()) {
            Trend t = trend.get();
            String url = switch (platform.toLowerCase()) {
                case "myntra"   -> t.getMyntraUrl();
                case "flipkart" -> t.getFlipkartUrl();
                default         -> t.getAmazonUrl();
            };
            log.debug("[AFFILIATE] Found URL in trends collection for product {}: {}", productId, url);
            return url;
        }

        // Check curated collection
        Optional<CuratedProduct> curated = curatedProductRepository.findById(productId);
        if (curated.isPresent()) {
            String url = curated.get().getShopUrl();
            log.debug("[AFFILIATE] Found URL in curated collection for product {}: {}", productId, url);
            return url;
        }

        log.warn("[AFFILIATE] Product {} not found in trends or curated collection", productId);
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // APPEND AFFILIATE TAG TO URL
    // ─────────────────────────────────────────────────────────────

    private String appendAffiliateTag(String originalUrl, String platform) {
        String tag       = switch (platform.toLowerCase()) {
            case "myntra"   -> MYNTRA_TAG;
            case "flipkart" -> FLIPKART_TAG;
            default         -> AMAZON_TAG;
        };
        // Amazon uses 'tag' param, others use 'ref'
        String paramName = platform.equalsIgnoreCase("amazon") ? "tag" : "ref";

        if (originalUrl.contains("?")) {
            return originalUrl + "&" + paramName + "=" + tag;
        } else {
            return originalUrl + "?" + paramName + "=" + tag;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BUILD FALLBACK SEARCH URL
    // ─────────────────────────────────────────────────────────────

    private String buildFallbackUrl(String platform, String query) {
        String safeQuery = query != null
                ? query.replaceAll("[^a-zA-Z0-9]", "+")
                : "fashion";

        return switch (platform != null ? platform.toLowerCase() : "amazon") {
            case "myntra"   -> "https://www.myntra.com/search?rawQuery=" + safeQuery;
            case "flipkart" -> "https://www.flipkart.com/search?q=" + safeQuery;
            default         -> "https://www.amazon.in/s?k=" + safeQuery
                    + "&tag=" + AMAZON_TAG;
        };
    }

    // ─────────────────────────────────────────────────────────────
    // EXTRACT REAL CLIENT IP
    // ─────────────────────────────────────────────────────────────

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For can be "client, proxy1, proxy2" — take first only
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}