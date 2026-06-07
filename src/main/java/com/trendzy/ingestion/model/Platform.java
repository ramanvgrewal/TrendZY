package com.trendzy.ingestion.model;

/**
 * Enumerates the social-media platforms that TrendZY V2 scrapes for trend signals.
 *
 * <p>Each platform maps to a dedicated scraper client (e.g. {@code InstagramExploreClient},
 * {@code PinterestExploreClient}) and is stored alongside every {@link TrendSignal}
 * for downstream filtering and analytics.
 */
public enum Platform {

    /** Instagram posts, reels, and stories. */
    INSTAGRAM,

    /** Pinterest pins and boards. */
    PINTEREST,

    /** Reddit posts and comments. */
    REDDIT,

    /** YouTube video descriptions and comments. */
    YOUTUBE,

    /** TikTok videos — reserved for future expansion. */
    TIKTOK
}
