package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.TrendResponse;
import com.trendzy.dto.response.TrendStatsResponse;
import com.trendzy.service.TrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    @GetMapping
    public ApiResponse<Page<TrendResponse>> getTrends(
            @RequestParam(name = "tier", required = false) String tier,
            @RequestParam(name = "vibe", required = false, defaultValue = "All") String vibe,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Page<TrendResponse> trends = trendService.getTrends(tier, vibe, PageRequest.of(page, size));
        return ApiResponse.<Page<TrendResponse>>builder()
                .success(true)
                .data(trends)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @GetMapping("/ticker")
    public ApiResponse<List<TrendResponse>> getTickerTrends() {
        List<TrendResponse> trends = trendService.getTickerTrends();
        return ApiResponse.<List<TrendResponse>>builder()
                .success(true)
                .data(trends)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @GetMapping("/top")
    public ApiResponse<TrendResponse> getTopTrend() {
        TrendResponse trend = trendService.getTopTrend();
        return ApiResponse.<TrendResponse>builder()
                .success(true)
                .data(trend)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @GetMapping("/stats")
    public ApiResponse<TrendStatsResponse> getStats() {
        TrendStatsResponse stats = trendService.getStats();
        return ApiResponse.<TrendStatsResponse>builder()
                .success(true)
                .data(stats)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<TrendResponse> getTrend(@PathVariable String id) {
        TrendResponse trend = trendService.getTrendById(id);
        return ApiResponse.<TrendResponse>builder()
                .success(true)
                .data(trend)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    @GetMapping("/{id}/related")
    public ApiResponse<List<TrendResponse>> getRelatedTrends(
            @PathVariable String id,
            @RequestParam(defaultValue = "6") int size) {
        List<TrendResponse> related = trendService.getRelatedTrends(id, size);
        return ApiResponse.<List<TrendResponse>>builder()
                .success(true)
                .data(related)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
