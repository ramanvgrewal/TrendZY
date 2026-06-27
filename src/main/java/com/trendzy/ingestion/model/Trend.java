package com.trendzy.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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

    // ── Core Identity — Fixed Mapping ──
    @Field("name")
    private String trendName;

    private String category;
    private String subcategory;
    private String aestheticId;

    // ── Intelligence Metrics ──
    private double trendScore;
    private double momentumScore;
    private double velocity;
    private String velocityLabel;

    private String tier;
    private List<String> vibeTags;

    // ── AI Analysis ──
    private String aiSummary;
    private List<String> whyTrending;
    private String indiaRelevanceNote;
    private boolean indiaRelevant;

    // ── Signal Evidence ──
    private long totalSignals;

    // ── Mapped Signal Evidence ──
    @Field("supportingSignals")
    private List<String> supportingSignalIds;

    // ── Product Enrichment Fields ──
    private String enrichmentStatus;
    private String enrichmentQuery;
    private List<String> aiBrandNames;

    // New Nested Object Architecture
    private ScrapedProducts products;

    private double estimatedPrice;
    private LocalDateTime firstDetectedAt;
    private LocalDateTime lastUpdatedAt;

    @Builder.Default
    private boolean active = true;

    // ── Nested Classes for MongoDB Mapping ──

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScrapedProducts {
        private ProductDetail amazon;
        private ProductDetail flipkart;
        private ProductDetail underdog;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductDetail {
        private String brandName;
        private String title;
        private Integer price;          // The selling/discounted price
        private Integer originalPrice;  // The MRP / crossed-out price
        private String shopUrl;
        private String imageUrl;
        private Boolean codAvailable;
    }
}