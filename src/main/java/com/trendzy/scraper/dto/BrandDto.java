package com.trendzy.scraper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intermediate DTO representing a brand discovered via Instagram.
 * This is a pipeline-internal object — it is NOT returned to API callers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {

    /** Instagram handle (without {@code @}). */
    private String username;

    /** Raw bio text scraped from the profile page. */
    private String bio;

    /**
     * External website URL extracted from the bio or the link-in-bio button.
     * May be a direct brand website or a link-aggregator (linktree, etc.).
     */
    private String websiteUrl;

    /** The original Instagram post URL that led to this brand. */
    private String sourcePostUrl;
}