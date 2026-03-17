package com.trendzy.service;

import com.trendzy.dto.response.CuratedResponse;
import com.trendzy.exception.TrendZyException;
import com.trendzy.model.mongo.CuratedProduct;
import com.trendzy.repository.mongo.CuratedProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CuratedService {

    private final CuratedProductRepository curatedProductRepository;

    // ─────────────────────────────────────────────────────────────
    // READ — PAGINATED LIST
    // ─────────────────────────────────────────────────────────────

    public Page<CuratedResponse> getCuratedProducts(String brand,
                                                    String vibeTag,
                                                    Pageable pageable) {
        boolean hasBrand = brand != null && !brand.isBlank();
        boolean hasVibe  = vibeTag != null && !vibeTag.isBlank()
                && !vibeTag.equalsIgnoreCase("All");

        log.info("[CURATED] Fetching products — brand: '{}', vibe: '{}', page: {}",
                hasBrand ? brand : "ALL",
                hasVibe  ? vibeTag : "ALL",
                pageable.getPageNumber());

        Page<CuratedProduct> products;

        if (hasBrand && hasVibe) {
            // Both filters active
            products = curatedProductRepository
                    .findByBrandNameAndVibeTagsContainingAndActiveTrue(brand, vibeTag, pageable);
        } else if (hasBrand) {
            products = curatedProductRepository
                    .findByBrandNameAndActiveTrue(brand, pageable);
        } else if (hasVibe) {
            products = curatedProductRepository
                    .findByVibeTagsContainingAndActiveTrue(vibeTag, pageable);
        } else {
            products = curatedProductRepository.findByActiveTrue(pageable);
        }

        log.info("[CURATED] Found {} products (page {}/{})",
                products.getNumberOfElements(),
                products.getNumber() + 1,
                products.getTotalPages());

        return products.map(this::mapToResponse);
    }

    public List<CuratedResponse> getAllCuratedProducts(String brand, String vibe) {
        // Logic to fetch from repository without Pageable
        return curatedProductRepository.findAll() // Or your filtered query
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // READ — SINGLE BY ID
    // ─────────────────────────────────────────────────────────────

    public CuratedResponse getCuratedProductById(String id) {
        log.info("[CURATED] Fetching product by id: {}", id);

        CuratedProduct product = curatedProductRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[CURATED] Product not found with id: {}", id);
                    return new TrendZyException("Curated product not found: " + id);
                });

        log.debug("[CURATED] Found product: '{}'", product.getProductName());
        return mapToResponse(product);
    }

    // ─────────────────────────────────────────────────────────────
    // READ — FEATURED (graceful — never throws)
    // ─────────────────────────────────────────────────────────────

    public Optional<CuratedResponse> getFeaturedProduct() {
        log.info("[CURATED] Fetching featured product");

        List<CuratedProduct> featuredList = curatedProductRepository
                .findAllByFeaturedTrueAndActiveTrue();

        if (featuredList.isEmpty()) {
            log.warn("[CURATED] No featured product found");
            return Optional.empty();
        }

        // Grab the first one safely from the list
        CuratedProduct featured = featuredList.get(0);
        log.info("[CURATED] Featured product: '{}'", featured.getProductName());

        return Optional.of(mapToResponse(featured));
    }

    // ─────────────────────────────────────────────────────────────
    // READ — BRANDS (from real DB data)
    // ─────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getBrands() {
        log.info("[CURATED] Fetching all brands from database");

        List<CuratedProduct> allActive = curatedProductRepository.findByActiveTrue();

        // Group by brandName, count products per brand
        Map<String, Long> brandCounts = allActive.stream()
                .filter(p -> p.getBrandName() != null && !p.getBrandName().isBlank())
                .collect(Collectors.groupingBy(
                        CuratedProduct::getBrandName,
                        Collectors.counting()
                ));

        // Build response — preserve insertion order, sort by count desc
        List<Map<String, Object>> brands = brandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    // Get brandLogo from first product of this brand
                    String logo = allActive.stream()
                            .filter(p -> entry.getKey().equals(p.getBrandName()))
                            .map(CuratedProduct::getBrandLogo)
                            .filter(l -> l != null && !l.isBlank())
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> brandMap = new LinkedHashMap<>();
                    brandMap.put("brandName", entry.getKey());
                    brandMap.put("productCount", entry.getValue());
                    brandMap.put("logoUrl", logo);
                    return brandMap;
                })
                .collect(Collectors.toList());

        log.info("[CURATED] Found {} unique brands", brands.size());
        return brands;
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────

    public CuratedResponse createCuratedProduct(CuratedProduct product) {
        log.info("[CURATED] Creating new curated product: '{}'", product.getProductName());

        product.setAddedAt(LocalDateTime.now());
        product.setActive(true);

        CuratedProduct saved = curatedProductRepository.save(product);

        log.info("[CURATED] ✅ Created curated product: '{}' | id: {}",
                saved.getProductName(), saved.getId());
        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE — all fields
    // ─────────────────────────────────────────────────────────────

    public CuratedResponse updateCuratedProduct(String id, CuratedProduct updates) {
        log.info("[CURATED] Updating curated product id: {}", id);

        CuratedProduct existing = curatedProductRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[CURATED] Cannot update — product not found: {}", id);
                    return new TrendZyException("Curated product not found: " + id);
                });

        // Update ALL fields if provided — never silently ignore
        if (updates.getProductName() != null)    existing.setProductName(updates.getProductName());
        if (updates.getBrandName() != null)       existing.setBrandName(updates.getBrandName());
        if (updates.getBrandLogo() != null)       existing.setBrandLogo(updates.getBrandLogo());
        if (updates.getCategory() != null)        existing.setCategory(updates.getCategory());
        if (updates.getSubcategory() != null)     existing.setSubcategory(updates.getSubcategory());
        if (updates.getVibeTags() != null)        existing.setVibeTags(updates.getVibeTags());
        if (updates.getImages() != null)          existing.setImages(updates.getImages());
        if (updates.getPrimaryImageUrl() != null) existing.setPrimaryImageUrl(updates.getPrimaryImageUrl());
        if (updates.getPrice() != null)           existing.setPrice(updates.getPrice());
        if (updates.getDescription() != null)     existing.setDescription(updates.getDescription());
        if (updates.getShopUrl() != null)         existing.setShopUrl(updates.getShopUrl());
        if (updates.getPlatform() != null)        existing.setPlatform(updates.getPlatform());

        CuratedProduct saved = curatedProductRepository.save(existing);

        log.info("[CURATED] ✅ Updated curated product: '{}' | id: {}",
                saved.getProductName(), saved.getId());
        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE — soft delete only
    // ─────────────────────────────────────────────────────────────

    public void deleteCuratedProduct(String id) {
        log.info("[CURATED] Soft-deleting curated product id: {}", id);

        CuratedProduct product = curatedProductRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[CURATED] Cannot delete — product not found: {}", id);
                    return new TrendZyException("Curated product not found: " + id);
                });

        product.setActive(false);
        curatedProductRepository.save(product);

        log.info("[CURATED] ✅ Soft-deleted product: '{}' | id: {}",
                product.getProductName(), id);
    }

    // ─────────────────────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────────────────────

    private CuratedResponse mapToResponse(CuratedProduct product) {
        return CuratedResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .brandName(product.getBrandName())
                .brandLogo(product.getBrandLogo())
                .category(product.getCategory())
                .subcategory(product.getSubcategory())
                .vibeTags(product.getVibeTags())
                .images(product.getImages())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .price(product.getPrice())
                .description(product.getDescription())
                .shopUrl(product.getShopUrl())
                .platform(product.getPlatform())
                .featured(product.isFeatured())
                .addedAt(product.getAddedAt())
                .build();
    }
}