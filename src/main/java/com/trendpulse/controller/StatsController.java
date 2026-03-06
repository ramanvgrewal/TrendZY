package com.trendpulse.controller;

import com.trendpulse.repository.RawSignalRepository;
import com.trendpulse.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final RawSignalRepository rawSignalRepository;
    private final TrendRepository trendRepository;

    @Value("${reddit.subreddits}")
    private String subreddits;

    /**
     * GET /api/stats — Dashboard stats
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalSignals = rawSignalRepository.count();
        long activeTrends = trendRepository.count();
        long indiaRelevantCount = trendRepository.countByIndiaRelevantTrue();
        int subredditsMonitored = subreddits.split(",").length;

        return ResponseEntity.ok(Map.of(
                "totalSignals", totalSignals,
                "activeTrends", activeTrends,
                "indiaRelevantCount", indiaRelevantCount,
                "subredditsMonitored", subredditsMonitored));
    }
}
