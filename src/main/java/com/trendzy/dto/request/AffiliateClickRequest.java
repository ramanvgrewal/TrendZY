package com.trendzy.dto.request;

import lombok.Data;

@Data
public class AffiliateClickRequest {
    private String productId;
    private String platform;
    private String source;
    private String productType; // "trend" | "curated"
}