package com.trendzy.ingestion.controller;

import com.trendzy.ingestion.service.SignalIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/ingestion")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

    private final SignalIngestionService ingestionService;

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerIngestion(
            @RequestParam(defaultValue = "STREETWEAR") String instagramSection) {

        // 🛡️ RAM Protection: Check the lock BEFORE triggering
        if (ingestionService.isIngestionRunning()) {
            log.warn("[CTRL] 🚨 Rejecting request: Ingestion cycle is already active.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "CONFLICT",
                    "message", "An ingestion cycle is currently running. Please wait for it to finish to protect server memory."
            ));
        }

        log.info("[CTRL] ✅ Triggering V2 ingestion cycle");
        ingestionService.runIngestionCycle(instagramSection);

        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "Ingestion cycle started asynchronously in the background."
        ));
    }
}