package com.trendpulse.controller;

import com.trendpulse.entity.UserPreference;
import com.trendpulse.service.PersonalisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class PersonalisationController {

    private final PersonalisationService personalisationService;

    /**
     * POST /api/session/view — Record a trend view
     * Body: {"sessionId": "abc123", "trendId": 5}
     */
    @PostMapping("/view")
    public ResponseEntity<Map<String, String>> recordView(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        Long trendId = Long.valueOf(body.get("trendId").toString());
        personalisationService.recordView(sessionId, trendId);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    /**
     * POST /api/session/buy-click — Record a buy-click
     * Body: {"sessionId": "abc123", "trendId": 5}
     */
    @PostMapping("/buy-click")
    public ResponseEntity<Map<String, String>> recordBuyClick(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        Long trendId = Long.valueOf(body.get("trendId").toString());
        personalisationService.recordBuyClick(sessionId, trendId);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    /**
     * GET /api/session/preferences/{sessionId} — Get user preferences
     */
    @GetMapping("/preferences/{sessionId}")
    public ResponseEntity<?> getPreferences(@PathVariable String sessionId) {
        return personalisationService.getPreferences(sessionId)
                .map(pref -> ResponseEntity.ok((Object) pref))
                .orElse(ResponseEntity.ok(Map.of("message", "No preferences found for session")));
    }
}
