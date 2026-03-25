package com.trendzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponse {
    private String id;
    private String productName;
    private String category;
    private String subcategory;
    
    private double trendScore;
    private double velocity;
    private String velocityLabel;
    private String tier;
    private List<String> vibeTags;
    
    private String aiSummary;
    private List<String> whyTrending;
    private String indiaRelevanceNote;
    
    private long totalSignals;
    private List<String> detectedSubreddits;
    private long youtubeVideoCount;
    
    private List<String> images; 
    private String imageUrl; 
    private String primaryImageUrl;
    private String shopUrl;
    private String amazonUrl;
    private String myntraUrl;
    private String flipkartUrl;
    private Double price;
    private String enrichmentStatus;
    private Double estimatedPrice;
    private Double originalPrice; 
    private String platform;
    
    private LocalDateTime firstDetectedAt;
    private LocalDateTime detectedAt; 
}
