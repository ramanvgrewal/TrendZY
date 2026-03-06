package com.trendpulse.controller;

import com.trendpulse.entity.Trend;
import com.trendpulse.repository.ProductLinkRepository;
import com.trendpulse.repository.TrendRepository;
import com.trendpulse.service.AiAnalysisService;
import com.trendpulse.service.RedditCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RedditCollectorService redditCollectorService;
    private final AiAnalysisService aiAnalysisService;
    private final TrendRepository trendRepository;
    private final ProductLinkRepository productLinkRepository;

    /**
     * POST /api/admin/collect — Manually trigger Reddit signal collection
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> triggerCollection() {
        int collected = redditCollectorService.manualCollect();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Reddit signal collection completed",
                "signalsCollected", collected));
    }

    /**
     * POST /api/admin/analyze — Manually trigger AI analysis of unprocessed signals
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> triggerAnalysis() {
        int processed = aiAnalysisService.analyzeSignals();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "AI analysis completed",
                "signalsProcessed", processed));
    }

    /**
     * POST /api/admin/regenerate-links — Delete all product links and regenerate
     * with correct URLs
     */
    @PostMapping("/regenerate-links")
    @Transactional
    public ResponseEntity<Map<String, Object>> regenerateLinks() {
        productLinkRepository.deleteAll();

        List<Trend> allTrends = trendRepository.findAll();
        int count = 0;
        for (Trend trend : allTrends) {
            trend.getProductLinks().clear();
            aiAnalysisService.generateAffiliateLinks(trend);
            count++;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All product links regenerated",
                "trendsProcessed", count));
    }
}
