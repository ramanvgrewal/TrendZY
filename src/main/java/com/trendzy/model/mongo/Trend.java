package com.trendzy.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "trends")
public class Trend {
    @Id
    private String id;

    private String productName;
    private String category;
    private String subcategory;

    private double trendScore; // 0-100
    private double velocity; // e.g., 38.0
    private String velocityLabel; // "+38%"

    private String tier; // "trending" | "rising"
    private List<String> vibeTags; // ["#Y2K", "#Streetwear"]

    private String aiSummary;
    private List<String> whyTrending;
    private String indiaRelevanceNote;

    private long totalSignals;
    private List<String> detectedSubreddits;
    private long youtubeVideoCount;

    // ── Product fingerprint (from AI analysis) ──
    private ProductFingerprint fingerprint;

    // ── Enrichment result ──
    private String imageUrl;
    private String shopUrl;         // single best product URL
    private String platform;        // winning platform: "myntra", "amazon", etc.
    private String enrichmentStatus; // PENDING, COMPLETED, NO_VALID_PRODUCT

    // ── Legacy per-platform URLs (deprecated, kept for backward compat) ──
    private String amazonUrl;
    private String flipkartUrl;
    private String myntraUrl;

    private double estimatedPrice;

    private LocalDateTime firstDetectedAt;
    private LocalDateTime lastUpdatedAt;

    private boolean active;
}
