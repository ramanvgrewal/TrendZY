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
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    
    private String trendId; // reference
    private String productName;
    
    private List<String> images;
    private String primaryImageUrl;
    
    private String amazonUrl;
    private String myntraUrl;
    private String flipkartUrl;
    
    private Double price;
    private Double originalPrice;
    private Double discount;
    
    private List<String> sizes;
    private List<String> colors;
    
    private String description;
    private String platform;
    
    private LocalDateTime enrichedAt;
    private String enrichmentStatus;
}
