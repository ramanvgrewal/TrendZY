package com.trendzy.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "curated_products")
public class CuratedProduct {
    @Id
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
    private boolean active;
    
    private LocalDateTime addedAt;
}
