package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.CuratedResponse;
import com.trendzy.service.CuratedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/curated")
@RequiredArgsConstructor
public class CuratedController {

    private final CuratedService curatedService;

    @GetMapping
    public ApiResponse<Page<CuratedResponse>> getCuratedProducts(
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "vibe", required = false, defaultValue = "All") String vibe,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "200") int size) {
        Page<CuratedResponse> products = curatedService.getCuratedProducts(brand, vibe, PageRequest.of(page, size));
        return ApiResponse.<Page<CuratedResponse>>builder()
                .success(true)
                .data(products)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<CuratedResponse>> getAllCuratedProducts(
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "vibe", required = false, defaultValue = "All") String vibe) {

        List<CuratedResponse> products = curatedService.getAllCuratedProducts(brand, vibe);

        return ApiResponse.<List<CuratedResponse>>builder()
                .success(true)
                .data(products)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CuratedResponse> getCuratedProduct(@PathVariable String id) {
        CuratedResponse product = curatedService.getCuratedProductById(id);
        return ApiResponse.<CuratedResponse>builder()
                .success(true)
                .data(product)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<CuratedResponse>> getFeatured() {
        Optional<CuratedResponse> featured = curatedService.getFeaturedProduct();
        return featured
                .map(f -> ResponseEntity.ok(ApiResponse.success(f)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/brands")
    public ApiResponse<List<Map<String, Object>>> getBrands() {
        return ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .data(curatedService.getBrands())
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
