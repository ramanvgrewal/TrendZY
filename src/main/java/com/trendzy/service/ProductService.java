package com.trendzy.service;

import com.trendzy.dto.response.ProductResponse;
import com.trendzy.exception.TrendZyException;
import com.trendzy.model.mongo.Product;
import com.trendzy.repository.mongo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new TrendZyException("Product not found"));
        return mapToResponse(product);
    }

    public ProductResponse getProductByTrendId(String trendId) {
        Product product = productRepository.findByTrendId(trendId)
                .orElseThrow(() -> new TrendZyException("Product not found for trend"));
        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .images(product.getImages())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .shopUrl(product.getShopUrl())
                .amazonUrl(product.getAmazonUrl())
                .myntraUrl(product.getMyntraUrl())
                .flipkartUrl(product.getFlipkartUrl())
                .meeshoUrl(product.getMeeshoUrl())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .matchScore(product.getMatchScore())
                .sizes(product.getSizes())
                .colors(product.getColors())
                .description(product.getDescription())
                .platform(product.getPlatform())
                .enrichmentStatus(product.getEnrichmentStatus())
                .build();
    }
}
