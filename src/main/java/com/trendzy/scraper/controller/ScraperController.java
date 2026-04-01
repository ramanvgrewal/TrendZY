package com.trendzy.scraper.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.scraper.dto.ProductDto;
import com.trendzy.scraper.instagram.InstagramSessionManager;
import com.trendzy.scraper.service.ScraperOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the Instagram product discovery pipeline.
 *
 * <p>All endpoints are restricted to {@code ROLE_ADMIN} via Spring Security's
 * method-level security ({@code @PreAuthorize}).  No changes to
 * {@code SecurityConfig} are needed — {@code @EnableMethodSecurity} is already
 * present in the existing config.
 *
 * <h3>Endpoints</h3>
 * <pre>
 * GET    /api/scraper/discover-products          → run full pipeline
 * GET    /api/scraper/session/status             → session health check
 * DELETE /api/scraper/session                    → invalidate session file
 * </pre>
 */
@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
@Slf4j
public class ScraperController {

    private final ScraperOrchestratorService orchestratorService;
    private final InstagramSessionManager    sessionManager;

    // ─────────────────────────────────────────────────────────────
    // MAIN DISCOVERY ENDPOINT
    // ─────────────────────────────────────────────────────────────

    /**
     * Runs the full Instagram → Brand → Product pipeline.
     *
     * <p><strong>Warning:</strong> this is a long-running synchronous call
     * (typically 3–8 minutes depending on how many brands are found and
     * how many pages Playwright must visit). In production, consider
     * running this as an async job and polling for results.
     *
     * @return list of {@link ProductDto} — up to 15 verified products
     */
    @GetMapping("/discover-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductDto>>> discoverProducts() {
        log.info("[CTRL] POST /api/scraper/discover-products — starting pipeline");
        long startMs = System.currentTimeMillis();

        List<ProductDto> products = orchestratorService.discoverProducts();

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        String message  = products.isEmpty()
                ? "Pipeline completed with 0 products — check session and logs"
                : "Discovered " + products.size() + " products in " + elapsedSec + "s";

        log.info("[CTRL] Pipeline finished: {} products in {}s", products.size(), elapsedSec);

        return ResponseEntity.ok(
                ApiResponse.<List<ProductDto>>builder()
                        .success(true)
                        .data(products)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────
    // SESSION STATUS
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns whether an Instagram session file exists on disk.
     * Use this before triggering a discovery run to confirm readiness.
     */
    @GetMapping("/session/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionStatus() {
        boolean ready  = sessionManager.sessionExists();
        String  status = ready ? "READY" : "LOGIN_REQUIRED";

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .data(Map.of(
                                "sessionReady", ready,
                                "status",       status,
                                "note",         ready
                                        ? "Session active — discovery pipeline is ready to run."
                                        : "No session found. The first pipeline run on a machine " +
                                        "with a display will open a browser for manual Instagram login."
                        ))
                        .message(status)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────
    // INVALIDATE SESSION
    // ─────────────────────────────────────────────────────────────

    /**
     * Deletes the stored Instagram session file.
     * The next pipeline run will open a headful browser for re-authentication.
     */
    @DeleteMapping("/session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> invalidateSession() {
        log.info("[CTRL] DELETE /api/scraper/session — invalidating session");
        sessionManager.invalidateSession();

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Session invalidated. Next pipeline run will require manual login.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}