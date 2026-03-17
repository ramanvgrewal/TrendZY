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
public class SearchResponse {
    private List<SearchResult> results;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    @Data
    @Builder
    public static class SearchResult {
        private String id;
        private String productName;
        private String category;
        private String tier;
        private String imageUrl;
        private Double price;
        private String brandName;
        private String shopUrl;       // ← add
        private String amazonUrl;     // ← add
        private String myntraUrl;     // ← add
        private List<String> vibeTags;
    }
}
