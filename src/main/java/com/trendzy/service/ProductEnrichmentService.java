package com.trendzy.service;

import com.trendzy.dto.response.ProductImageResult;
import com.trendzy.model.mongo.Product;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.mongo.ProductRepository;
import com.trendzy.repository.mongo.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEnrichmentService {

    private final TrendRepository trendRepository;
    private final ProductRepository productRepository;
    private final ProductImageExtractorService imageExtractorService;

    public int enrichProductsBatch() {
        List<Trend> unenriched = trendRepository.findByImageUrlIsNullAndActiveTrue();

        if (unenriched.isEmpty()) {
            log.info("[ENRICH] No unenriched trends found");
            return 0;
        }

        log.info("[ENRICH] Found {} unenriched trends", unenriched.size());
        int enrichedCount = 0;

        for (Trend trend : unenriched) {
            try {
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
     * Enrich a single trend: build ecommerce search URLs → get first product link per site →
     * load product page → extract og:image. Store only real product image URLs and product page URLs.
     * No placeholders, search-page images, or stock APIs.
     */
    private void enrichTrend(Trend trend) {
        log.info("[ENRICH] Enriching '{}'", trend.getProductName());

        String rawQuery = (trend.getProductName() + " " + (trend.getCategory() != null ? trend.getCategory() : ""))
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .trim();
        String urlQuery = rawQuery.replace(" ", "+");

        String amazonSearchUrl = "https://www.amazon.in/s?k=" + urlQuery;
        String myntraSearchUrl = "https://www.myntra.com/search?rawQuery=" + urlQuery;
        String flipkartSearchUrl = "https://www.flipkart.com/search?q=" + urlQuery;

        String imageUrl = null;
        String amazonProductUrl = null;
        String myntraProductUrl = null;
        String flipkartProductUrl = null;

        ProductImageResult result = imageExtractorService.extractProductImageFromSearchUrl(amazonSearchUrl);
        if (result != null && result.getImageUrl() != null) {
            imageUrl = result.getImageUrl();
            amazonProductUrl = result.getProductPageUrl();
            log.debug("[ENRICH] Got image from Amazon product page for '{}'", trend.getProductName());
        }
        if (imageUrl == null) {
            result = imageExtractorService.extractProductImageFromSearchUrl(myntraSearchUrl);
            if (result != null && result.getImageUrl() != null) {
                imageUrl = result.getImageUrl();
                myntraProductUrl = result.getProductPageUrl();
                log.debug("[ENRICH] Got image from Myntra product page for '{}'", trend.getProductName());
            }
        }
        if (imageUrl == null) {
            result = imageExtractorService.extractProductImageFromSearchUrl(flipkartSearchUrl);
            if (result != null && result.getImageUrl() != null) {
                imageUrl = result.getImageUrl();
                flipkartProductUrl = result.getProductPageUrl();
                log.debug("[ENRICH] Got image from Flipkart product page for '{}'", trend.getProductName());
            }
        }

        if (imageUrl == null) {
            log.warn("[ENRICH] No product image found for '{}' — saving without image (no placeholder)", trend.getProductName());
        }

        String finalAmazonUrl = amazonProductUrl != null ? amazonProductUrl : amazonSearchUrl;
        String finalMyntraUrl = myntraProductUrl != null ? myntraProductUrl : myntraSearchUrl;
        String finalFlipkartUrl = flipkartProductUrl != null ? flipkartProductUrl : flipkartSearchUrl;

        List<String> imageList = new ArrayList<>();
        if (imageUrl != null) {
            imageList.add(imageUrl);
        }

        Product product = Product.builder()
                .trendId(trend.getId())
                .productName(trend.getProductName())
                .primaryImageUrl(imageUrl)
                .images(imageList)
                .amazonUrl(finalAmazonUrl)
                .myntraUrl(finalMyntraUrl)
                .flipkartUrl(finalFlipkartUrl)
                .price(trend.getEstimatedPrice() > 0 ? trend.getEstimatedPrice() : null)
                .originalPrice(null)
                .discount(null)
                .sizes(List.of())
                .colors(List.of())
                .description(trend.getAiSummary())
                .platform(amazonProductUrl != null ? "amazon" : (myntraProductUrl != null ? "myntra" : "flipkart"))
                .enrichedAt(LocalDateTime.now())
                .enrichmentStatus("COMPLETED")
                .build();

        productRepository.save(product);

        trend.setImageUrl(imageUrl);
        trend.setAmazonUrl(finalAmazonUrl);
        trend.setMyntraUrl(finalMyntraUrl);
        trend.setFlipkartUrl(finalFlipkartUrl);
        trend.setPlatform(product.getPlatform());
        trend.setLastUpdatedAt(LocalDateTime.now());
        trendRepository.save(trend);

        log.info("[ENRICH] Trend updated '{}' | image: {}", trend.getProductName(), imageUrl != null ? "set" : "none");
    }
}
