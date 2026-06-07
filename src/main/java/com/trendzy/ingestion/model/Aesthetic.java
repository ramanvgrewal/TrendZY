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
 * Represents a named fashion aesthetic / micro-trend cluster.
 *
 * <p>Aesthetics are the high-level groupings that TrendZY uses to organise
 * individual signals into coherent trend narratives. Examples:
 * <ul>
 *   <li><strong>Y2K Revival</strong> — low-rise jeans, butterfly clips, baby tees</li>
 *   <li><strong>Dark Academia</strong> — tweed, earth tones, vintage blazers</li>
 *   <li><strong>Cottagecore</strong> — linen, floral prints, pastoral palette</li>
 *   <li><strong>Gorpcore</strong> — outdoor / utility, Arc'teryx vibes</li>
 * </ul>
 *
 * <p>The AI worker creates and updates aesthetics as it processes {@link TrendSignal}s.
 * The frontend consumes aesthetics to render trend-cluster cards.
 *
 * <p><strong>Collection:</strong> {@code aesthetics}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aesthetics")
public class Aesthetic {

    @Id
    private String id;

    /** Display name (e.g. "Y2K Revival", "Dark Academia"). */
    private String name;

    /**
     * URL-safe slug (e.g. "y2k-revival", "dark-academia").
     * Unique index for REST API lookups.
     */
    @Indexed(unique = true)
    private String slug;

    /** 1–3 sentence description of the aesthetic, written by the AI worker. */
    private String description;

    /**
     * Representative colour palette in hex codes.
     * Used by the frontend to render trend cards with thematic colouring.
     * Example: ["#1a1a2e", "#e94560", "#0f3460"]
     */
    @Builder.Default
    private List<String> colorPalette = new ArrayList<>();

    /**
     * Hashtags commonly associated with this aesthetic.
     * Stored without the leading {@code #} (e.g. "y2kfashion", "cottagecore").
     */
    @Builder.Default
    private List<String> relatedHashtags = new ArrayList<>();

    /**
     * Total number of {@link TrendSignal}s that the AI worker has classified
     * under this aesthetic. Updated incrementally.
     */
    @Builder.Default
    private long signalCount = 0L;

    /**
     * Composite trend score (0–100) computed by the AI worker.
     * Higher = more momentum / virality right now.
     */
    @Builder.Default
    private double trendScore = 0.0;

    /** When this aesthetic was first identified. */
    @CreatedDate
    private Instant createdAt;

    /** When this aesthetic's metrics were last refreshed. */
    @LastModifiedDate
    private Instant updatedAt;

    /** Soft-delete flag. Inactive aesthetics are excluded from API responses. */
    @Builder.Default
    private boolean active = true;
}
