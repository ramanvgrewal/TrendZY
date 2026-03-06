package com.trendpulse.controller;

import com.trendpulse.dto.TrendResponse;
import com.trendpulse.service.TrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    /**
     * GET /api/trends — Discovery feed
     * Supports: ?category=Fashion &audience=Gen-Z &gender=female &pricePoint=budget
     * &indiaRelevant=true &sessionId=abc123
     */
    @GetMapping
    public ResponseEntity<List<TrendResponse>> getAllTrends(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String pricePoint,
            @RequestParam(required = false) Boolean indiaRelevant,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity
                .ok(trendService.getAllTrends(category, audience, gender, pricePoint, indiaRelevant, sessionId));
    }

    /**
     * GET /api/trends/rising — Early-stage trends before they blow up
     */
    @GetMapping("/rising")
    public ResponseEntity<List<TrendResponse>> getRisingTrends() {
        return ResponseEntity.ok(trendService.getRisingTrends());
    }

    /**
     * GET /api/trends/categories — Available filter categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(trendService.getCategories());
    }

    /**
     * GET /api/trends/{id} — Single trend detail with product links
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrendResponse> getTrendById(@PathVariable Long id) {
        return trendService.getTrendById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
