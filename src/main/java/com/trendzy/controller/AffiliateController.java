package com.trendzy.controller;

import com.trendzy.dto.request.AffiliateClickRequest;
import com.trendzy.dto.response.ApiResponse;
import com.trendzy.service.AffiliateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/affiliate")
@RequiredArgsConstructor
@Slf4j
public class AffiliateController {

    private final AffiliateService affiliateService;

    // ─────────────────────────────────────────────────────────────
    // GET AFFILIATE LINK
    // GET /api/affiliate/link?productId={id}&platform={platform}
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/link")
    public ResponseEntity<ApiResponse<Map<String, String>>> getLink(
            @RequestParam String productId,
            @RequestParam(defaultValue = "amazon") String platform) {

        log.info("[AFFILIATE CTRL] Get link — productId: {}, platform: {}",
                productId, platform);

        String url = affiliateService.generateAffiliateLink(productId, platform);
        return ResponseEntity.ok(ApiResponse.success(Map.of("affiliateUrl", url)));
    }

    // ─────────────────────────────────────────────────────────────
    // TRACK CLICK
    // POST /api/affiliate/click
    // Body: { productId, platform, source, productType }
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/click")
    public ResponseEntity<ApiResponse<Void>> trackClick(
            @RequestBody AffiliateClickRequest request,
            HttpServletRequest httpRequest) {

        log.info("[AFFILIATE CTRL] Track click — productId: {}, platform: {}, source: {}",
                request.getProductId(),
                request.getPlatform(),
                request.getSource());

        affiliateService.trackClick(
                request.getProductId(),
                request.getPlatform(),
                request.getSource(),
                request.getProductType(),
                httpRequest
        );

        return ResponseEntity.ok(ApiResponse.success(null, "Click tracked"));
    }
}