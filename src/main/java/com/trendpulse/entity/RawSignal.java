package com.trendpulse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source = "reddit";

    private String subreddit;

    @Column(name = "post_id")
    private String postId;

    @Column(name = "post_title", columnDefinition = "TEXT")
    private String postTitle;

    @Column(name = "comment_body", columnDefinition = "TEXT")
    private String commentBody;

    private String author;

    @Column(name = "keyword_matched")
    private String keywordMatched;

    @Column(name = "product_mentioned")
    private String productMentioned;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(nullable = false)
    private Boolean processed = false;

    @PrePersist
    protected void onCreate() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}
