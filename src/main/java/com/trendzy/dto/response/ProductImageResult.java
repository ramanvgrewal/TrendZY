package com.trendzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of extracting a product image from an ecommerce site.
 * imageUrl: direct product image (og:image or main product image from product page).
 * productPageUrl: the actual product page URL (not the search page).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResult {
    private String imageUrl;
    private String productPageUrl;
}
