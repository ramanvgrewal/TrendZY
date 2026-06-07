package com.trendzy.ingestion.controller;

import com.trendzy.ingestion.service.SignalIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoint to manually trigger the V2 ingestion pipeline.
 */
@RestController
@RequestMapping("/api/v2/ingestion")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

    private final SignalIngestionService ingestionService;

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerIngestion(
            @RequestParam(defaultValue = "STREETWEAR") String instagramSection,
            @RequestParam(defaultValue = "vintage streetwear") String pinterestQuery) {
        
        log.info("[CTRL] Triggering V2 ingestion cycle");
        ingestionService.runIngestionCycle(instagramSection, pinterestQuery);
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "ACCEPTED",
            "message", "Ingestion cycle started asynchronously"
        ));
    }
}
