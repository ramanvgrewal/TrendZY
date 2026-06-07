package com.trendzy.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent representation of a fashion brand or creator discovered during scraping.
 *
 * <p>This entity is persisted to MongoDB and serves as the system-of-record for all brands across
 * every platform (Instagram, Pinterest, etc.).
 *
 * <p>Brands are deduplicated by {@link #profileUrl} (unique index) so that a brand
 * discovered via Instagram and later seen on Pinterest is recognised as the same entity.
 *
 * <p><strong>Collection:</strong> {@code brands}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "brands")
public class Brand {

    @Id
    private String id;

    /** Human-readable brand / creator name (e.g. "Bewakoof", "The Souled Store"). */
    private String name;

    /**
     * URL-safe slug derived from the name (e.g. "bewakoof", "the-souled-store").
     * Indexed for fast lookups via REST API.
     */
    @Indexed(unique = true)
    private String slug;

    /** The primary platform where this brand was first discovered. */
    private Platform platform;

    /**
     * Canonical profile URL on the discovery platform.
     * (e.g. "https://www.instagram.com/bewakoof/")
     * Unique index prevents duplicate brand entries.
     */
    @Indexed(unique = true)
    private String profileUrl;

    /** Brand's own website or link-in-bio destination. May be null for new brands. */
    private String websiteUrl;

    /** Raw bio / description text scraped from the profile. */
    private String bio;

    /** Approximate follower count at time of last scrape. May be null if unavailable. */
    private Long followerCount;

    /** High-level category: "Streetwear", "Sustainable", "Sneakers", etc. */
    private String category;

    /**
     * Freeform tags assigned by the AI or by human curation.
     * Examples: ["d2c", "oversized", "gen-z", "indian"]
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /** When this brand was first added to the system. */
    @CreatedDate
    private Instant discoveredAt;

    /** When the scraper last visited this brand's profile. */
    @LastModifiedDate
    private Instant lastScrapedAt;

    /** Soft-delete flag. Inactive brands are excluded from pipeline runs. */
    @Builder.Default
    private boolean active = true;
}
