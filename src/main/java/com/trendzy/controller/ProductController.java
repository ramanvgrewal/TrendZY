package com.trendzy.controller;

import com.trendzy.dto.response.ApiResponse;
import com.trendzy.dto.response.ProductResponse;
import com.trendzy.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ApiResponse.<ProductResponse>builder()
                .success(true)
                .data(product)
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
