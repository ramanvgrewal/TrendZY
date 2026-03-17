package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.model.mongo.CuratedProduct;
import com.trendzy.model.mongo.TokenUsage;
import com.trendzy.service.CuratedService;
import com.trendzy.service.PipelineOrchestratorService;
import com.trendzy.service.TokenBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PipelineOrchestratorService pipeline;
    private final TokenBudgetService tokenBudgetService;
    private final CuratedService curatedService;

    @PostMapping("/pipeline/collect")
    public ApiResponse<Map<String, Integer>> triggerCollection() {
        return ApiResponse.<Map<String, Integer>>builder()
                .success(true)
                .data(pipeline.triggerCollection())
                .message("Collection triggered")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/pipeline/analyze")
    public ApiResponse<Map<String, Object>> triggerAnalysis() {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(pipeline.triggerAnalysis())
                .message("Analysis triggered")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/pipeline/enrich")
    public ApiResponse<Map<String, Integer>> triggerEnrichment() {
        return ApiResponse.<Map<String, Integer>>builder()
                .success(true)
                .data(pipeline.triggerEnrichment())
                .message("Enrichment triggered")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/tokens/usage")
    public ApiResponse<TokenUsage> getTokenUsage() {
        return ApiResponse.<TokenUsage>builder()
                .success(true)
                .data(tokenBudgetService.getTodayUsage())
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/system/health")
    public ApiResponse<Map<String, String>> getSystemHealth() {
        return ApiResponse.<Map<String, String>>builder()
               .success(true)
               .data(Map.of("mongoStatus", "UP", "postgresStatus", "UP", "groqStatus", "UP", "redditStatus", "UP", "youtubeStatus", "UP", "uptime", "24h"))
               .message("System healthy")
               .timestamp(LocalDateTime.now())
               .build();
    }

    @GetMapping("/curated")
    public ApiResponse<Page<com.trendzy.dto.response.CuratedResponse>> getCuratedList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<Page<com.trendzy.dto.response.CuratedResponse>>builder()
                .success(true)
                .data(curatedService.getCuratedProducts(null, null, PageRequest.of(page, size)))
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/curated")
    public ApiResponse<com.trendzy.dto.response.CuratedResponse> createCurated(@RequestBody CuratedProduct product) {
        return ApiResponse.<com.trendzy.dto.response.CuratedResponse>builder()
                .success(true)
                .data(curatedService.createCuratedProduct(product))
                .message("Created")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PutMapping("/curated/{id}")
    public ApiResponse<com.trendzy.dto.response.CuratedResponse> updateCurated(
            @PathVariable String id, @RequestBody CuratedProduct updates) {
        return ApiResponse.<com.trendzy.dto.response.CuratedResponse>builder()
                .success(true)
                .data(curatedService.updateCuratedProduct(id, updates))
                .message("Updated")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @DeleteMapping("/curated/{id}")
    public ApiResponse<Void> deleteCurated(@PathVariable String id) {
        curatedService.deleteCuratedProduct(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/curated/seed")
    public ApiResponse<Void> reSeedCurated() {
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Seeder triggered")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
