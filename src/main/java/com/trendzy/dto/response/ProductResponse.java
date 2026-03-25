package com.trendzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String productName;
    private List<String> images;
    private String primaryImageUrl;
    private String shopUrl;
    private String amazonUrl;
    private String myntraUrl;
    private String flipkartUrl;
    private String meeshoUrl;
    private Double price;
    private Double originalPrice;
    private Double discount;
    private double matchScore;
    private List<String> sizes;
    private List<String> colors;
    private String description;
    private String platform;
    private String enrichmentStatus;
}
