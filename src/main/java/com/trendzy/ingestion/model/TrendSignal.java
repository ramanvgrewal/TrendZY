package com.trendzy.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A raw trend signal captured from a social-media platform.
 *
 * <p>This is the foundational data unit for TrendZY V2's ingestion pipeline.
 * Each signal represents a single social-media post (Instagram post/reel,
 * Pinterest pin, etc.) that may contain fashion-trend indicators.
 *
 * <p>Signals flow through the pipeline as follows:
 * <ol>
 *   <li>Scraper client extracts raw data → persisted with {@code processedByAi = false}</li>
 *   <li>Kafka event published to {@code raw-signals-topic}</li>
 *   <li>Python AI worker picks up the event, analyses the signal, sets {@code processedByAi = true}</li>
 * </ol>
 *
 * <p><strong>Collection:</strong> {@code v2_signals} — intentionally separate from the
 * legacy {@code signals} collection used by Reddit/YouTube V1 pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "v2_signals")
@CompoundIndexes({
        @CompoundIndex(name = "idx_platform_processed", def = "{'platform': 1, 'processedByAi': 1}"),
        @CompoundIndex(name = "idx_platform_collectedAt", def = "{'platform': 1, 'collectedAt': -1}")
})
public class TrendSignal {

    @Id
    private String id;

    /**
     * The platform this signal was scraped from.
     * Never null — every signal must declare its origin.
     */
    private Platform platform;

    /**
     * Canonical URL of the source content (post URL, pin URL, etc.).
     * Used as a natural deduplication key — unique index enforced at the DB level.
     */
    @Indexed(unique = true)
    private String sourceUrl;

    /**
     * Raw text content — Instagram caption, Pinterest pin description, etc.
     * May contain hashtags, mentions, and emojis. Processed downstream by the AI worker.
     */
    private String rawText;

    /**
     * Hashtags extracted from the raw text.
     * Stored without the leading {@code #} symbol (e.g. {@code "streetwear"} not {@code "#streetwear"}).
     */
    @Builder.Default
    private List<String> hashtags = new ArrayList<>();

    /**
     * Platform-specific engagement metric:
     * <ul>
     *   <li>Instagram: like count</li>
     *   <li>Pinterest: repin count (or save count)</li>
     *   <li>Reddit: upvote count</li>
     *   <li>YouTube: view count</li>
     * </ul>
     */
    private long engagementScore;

    /** Username / handle of the content author (without {@code @} prefix). */
    private String authorUsername;

    /** Direct URL to the primary media asset (image or video thumbnail). */
    private String mediaUrl;

    /** Timestamp when the scraper collected this signal. */
    @CreatedDate
    private Instant collectedAt;

    /**
     * {@code false} when freshly ingested; set to {@code true} by the AI worker
     * after trend analysis is complete.
     */
    @Builder.Default
    private boolean processedByAi = false;
}
