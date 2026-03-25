package com.trendzy.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured identity model derived from AI analysis.
 * Used by the scoring engine to validate product candidates against the trend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFingerprint {
    private String brand;           // e.g. "Nike", "Zara"
    private String productType;     // e.g. "sneakers", "hoodie", "tote bag"
    private String color;           // e.g. "black", "white", "multicolor"
    private String gender;          // e.g. "men", "women", "unisex"
    private List<String> keywords;  // e.g. ["retro", "chunky", "platform"]
}
