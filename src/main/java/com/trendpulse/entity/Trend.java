package com.trendpulse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    private String category;

    @Column(name = "trend_score", nullable = false)
    private Double trendScore = 0.0;

    @Column(nullable = false)
    private Double velocity = 0.0;

    @Column(name = "velocity_label")
    private String velocityLabel;

    @Column(nullable = false)
    private Double confidence = 0.0;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "mention_count_this_week", nullable = false)
    private Integer mentionCountThisWeek = 0;

    @Column(name = "mention_count_last_week", nullable = false)
    private Integer mentionCountLastWeek = 0;

    @Column(name = "audience_tag")
    private String audienceTag = "Gen-Z";

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    // ── R3: New fields ──

    @Column(name = "brand_mention")
    private String brandMention;

    @Column(name = "price_point")
    private String pricePoint;

    private String gender;

    @Column(name = "india_relevant")
    private Boolean indiaRelevant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "trend", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductLink> productLinks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
