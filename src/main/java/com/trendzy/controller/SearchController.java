package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.SearchResponse;
import com.trendzy.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/autocomplete")
    public ApiResponse<List<SearchResponse.SearchResult>> autocomplete(@RequestParam String q) {
        List<SearchResponse.SearchResult> results = searchService.autocomplete(q);
        return ApiResponse.<List<SearchResponse.SearchResult>>builder()
                .success(true)
                .data(results)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping
    public ApiResponse<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "All") String vibe,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        SearchResponse response = searchService.search(q, type, vibe, PageRequest.of(page, size));
        return ApiResponse.<SearchResponse>builder()
                .success(true)
                .data(response)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/trending-queries")
    public ApiResponse<List<String>> getTrendingQueries() {
        return ApiResponse.<List<String>>builder()
                .success(true)
                .data(searchService.getTrendingQueries())
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
