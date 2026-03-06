package com.trendpulse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "liked_categories", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> likedCategories = new ArrayList<>();

    @Column(name = "viewed_trend_ids", columnDefinition = "BIGINT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<Long> viewedTrendIds = new ArrayList<>();

    @Column(name = "clicked_buy_ids", columnDefinition = "BIGINT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<Long> clickedBuyIds = new ArrayList<>();

    @Column(name = "audience_tag")
    private String audienceTag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
