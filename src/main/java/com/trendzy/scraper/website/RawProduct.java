package com.trendzy.scraper.website;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal pipeline model representing a product scraped from a brand website,
 * before validation and DTO transformation.
 *
 * <p>This class is package-scoped to the {@code website} sub-package and is
 * intentionally separate from {@link com.trendzy.scraper.dto.ProductDto} to
 * keep raw scraped data decoupled from the API contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawProduct {

    /** Raw product title as it appears on the website. */
    private String productName;

    /** Price as a {@code double} to preserve precision before INR conversion. */
    private double price;

    /** Full CDN or absolute image URL. */
    private String imageUrl;

    /** Direct product page URL. */
    private String productUrl;

    /** Set to {@code true} after {@link com.trendzy.scraper.validator.ProductValidator} passes. */
    @Builder.Default
    private boolean validated = false;
}