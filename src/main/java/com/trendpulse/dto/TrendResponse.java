package com.trendpulse.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendResponse {

    private Long id;
    private String productName;
    private String category;
    private Double trendScore;
    private Double velocity;
    private String velocityLabel;
    private Double confidence;
    private String aiExplanation;
    private Integer mentionCountThisWeek;
    private Integer mentionCountLastWeek;
    private String audienceTag;
    private String imageUrl;

    // ── R3: New fields ──
    private String brandMention;
    private String pricePoint;
    private String gender;
    private Boolean indiaRelevant;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductLinkDto> productLinks;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductLinkDto {
        private Long id;
        private String platform;
        private String affiliateUrl;
        private String priceRange;
    }
}
