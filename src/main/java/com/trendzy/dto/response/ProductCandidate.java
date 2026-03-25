package com.trendzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transient DTO representing a single scraped product candidate.
 * Scored and ranked by ProductScoringService before selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCandidate {

    public enum Platform {
        MYNTRA, AMAZON, FLIPKART, MEESHO
    }

    private Platform platform;
    private String title;
    private String url;
    private String imageUrl;
    private Double price;

    @Builder.Default
    private double score = 0.0;

    @Builder.Default
    private boolean rejected = false;

    private String rejectionReason;
}
