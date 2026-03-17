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
public class CuratedResponse {
    private String id;
    private String productName;
    private String brandName;
    private String brandLogo;
    
    private String category;
    private String subcategory;
    private List<String> vibeTags;
    
    private List<String> images;
    private String primaryImageUrl;
    
    private Double price;
    private String description;
    
    private String shopUrl;
    private String platform;
    
    private boolean featured;
    private LocalDateTime addedAt;
}
