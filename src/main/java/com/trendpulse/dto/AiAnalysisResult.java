package com.trendpulse.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResult {

    @JsonProperty("trendScore")
    private Double trendScore;

    @JsonProperty("velocity")
    private Double velocity;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("category")
    private String category;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("audienceTag")
    private String audienceTag;

    // ── R3: New fields ──

    @JsonProperty("brandMention")
    private String brandMention;

    @JsonProperty("pricePoint")
    private String pricePoint;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("indiaRelevant")
    private Boolean indiaRelevant;
}
