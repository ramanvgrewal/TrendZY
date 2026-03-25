package com.trendzy.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trendzy.model.mongo.CuratedProduct;
import com.trendzy.repository.mongo.CuratedProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class CuratedProductSeeder {

    private final CuratedProductRepository curatedProductRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void seedData() {
        List<Map<String, Object>> seedData = loadSeedFile();
        if (seedData == null || seedData.isEmpty()) {
            log.warn("[SEEDER] curated_seed.json is empty or missing — skipping");
            return;
        }

        log.info("[SEEDER] Syncing {} products from curated_seed.json...", seedData.size());

        int created = 0;
        int updated = 0;
        int skipped = 0;

        // ── Track every valid key present in the seed file ────────
        Set<String> seedKeys = new HashSet<>();

        for (Map<String, Object> item : seedData) {
            try {
                CuratedProduct incoming = mapToProduct(item);
                if (incoming == null) {
                    skipped++;
                    continue;
                }

                // Register this product as "still in seed"
                seedKeys.add(compositeKey(incoming.getProductName(), incoming.getBrandName()));

                Optional<CuratedProduct> existing = curatedProductRepository
                        .findByProductNameAndBrandName(
                                incoming.getProductName(),
                                incoming.getBrandName());

                if (existing.isPresent()) {
                    CuratedProduct current = existing.get();
                    boolean changed = applyUpdates(current, incoming);

                    if (changed) {
                        curatedProductRepository.save(current);
                        updated++;
                        log.info("[SEEDER] 🔄 Updated: '{}' by '{}'",
                                current.getProductName(), current.getBrandName());
                    } else {
                        skipped++;
                        log.debug("[SEEDER] No changes for: '{}' — skipping",
                                incoming.getProductName());
                    }

                } else {
                    curatedProductRepository.save(incoming);
                    created++;
                    log.info("[SEEDER] ✅ Created: '{}' by '{}'",
                            incoming.getProductName(), incoming.getBrandName());
                }

            } catch (Exception e) {
                log.error("[SEEDER] Failed processing '{}': {}",
                        item.get("productName"), e.getMessage());
                skipped++;
            }
        }

        // ── Hard delete products removed from the seed file ───────
        int deleted = deleteRemovedProducts(seedKeys);

        log.info("[SEEDER] ✅ Sync complete — created: {}, updated: {}, unchanged: {}, deleted: {}",
                created, updated, skipped, deleted);
    }

    // ─────────────────────────────────────────────────────────────
    // HARD DELETE REMOVED PRODUCTS
    // ─────────────────────────────────────────────────────────────

    private int deleteRemovedProducts(Set<String> seedKeys) {
        List<CuratedProduct> allProducts = curatedProductRepository.findAll();
        int deleted = 0;

        for (CuratedProduct product : allProducts) {
            String key = compositeKey(product.getProductName(), product.getBrandName());
            if (!seedKeys.contains(key)) {
                curatedProductRepository.delete(product);
                deleted++;
                log.info("[SEEDER] 🗑️ Deleted (removed from seed): '{}' by '{}'",
                        product.getProductName(), product.getBrandName());
            }
        }

        return deleted;
    }

    private String compositeKey(String productName, String brandName) {
        return (productName + "::" + brandName).toLowerCase();
    }

    // ─────────────────────────────────────────────────────────────
    // APPLY UPDATES — returns true if anything changed
    // ─────────────────────────────────────────────────────────────

    private boolean applyUpdates(CuratedProduct existing, CuratedProduct incoming) {
        boolean changed = false;

        if (hasChanged(existing.getPrimaryImageUrl(), incoming.getPrimaryImageUrl())) {
            existing.setPrimaryImageUrl(incoming.getPrimaryImageUrl());
            existing.setImages(incoming.getImages());
            changed = true;
        }
        if (hasChanged(existing.getShopUrl(), incoming.getShopUrl())) {
            existing.setShopUrl(incoming.getShopUrl());
            changed = true;
        }
        if (hasChanged(existing.getDescription(), incoming.getDescription())) {
            existing.setDescription(incoming.getDescription());
            changed = true;
        }
        if (hasChanged(existing.getCategory(), incoming.getCategory())) {
            existing.setCategory(incoming.getCategory());
            changed = true;
        }
        if (hasChanged(existing.getSubcategory(), incoming.getSubcategory())) {
            existing.setSubcategory(incoming.getSubcategory());
            changed = true;
        }
        if (!pricesEqual(existing.getPrice(), incoming.getPrice())) {
            existing.setPrice(incoming.getPrice());
            changed = true;
        }
        if (hasChanged(existing.getBrandLogo(), incoming.getBrandLogo())) {
            existing.setBrandLogo(incoming.getBrandLogo());
            changed = true;
        }
        if (incoming.getVibeTags() != null &&
                !incoming.getVibeTags().equals(existing.getVibeTags())) {
            existing.setVibeTags(incoming.getVibeTags());
            changed = true;
        }
        if (existing.isFeatured() != incoming.isFeatured()) {
            existing.setFeatured(incoming.isFeatured());
            changed = true;
        }

        return changed;
    }

    private boolean hasChanged(String existing, String incoming) {
        if (incoming == null) return false;
        return !incoming.equals(existing);
    }

    private boolean pricesEqual(Double existing, Double incoming) {
        if (incoming == null) return true;
        if (existing == null) return false;
        return existing.equals(incoming);
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD JSON FILE
    // ─────────────────────────────────────────────────────────────

    private List<Map<String, Object>> loadSeedFile() {
        try {
            ClassPathResource resource = new ClassPathResource("curated_seed.json");
            if (!resource.exists()) {
                log.error("[SEEDER] curated_seed.json not found in src/main/resources");
                return null;
            }
            InputStream inputStream = resource.getInputStream();
            return objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("[SEEDER] Failed to read curated_seed.json: {}", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MAP JSON → MODEL
    // ─────────────────────────────────────────────────────────────

    private CuratedProduct mapToProduct(Map<String, Object> item) {
        String productName = getString(item, "productName");
        String brandName   = getString(item, "brandName");

        if (productName == null || productName.isBlank()) {
            log.warn("[SEEDER] Skipping item with missing productName");
            return null;
        }
        if (brandName == null || brandName.isBlank()) {
            log.warn("[SEEDER] Skipping item with missing brandName");
            return null;
        }

        String imageUrl    = getString(item, "imageUrl");
        String websiteLink = getString(item, "websiteLink");
        String category    = getString(item, "category");
        String productType = getString(item, "productType");
        String description = getString(item, "description");
        String brandLogo   = getString(item, "brandLogo");
        Boolean featured   = getBoolean(item, "featured");
        Double priceInr    = getDouble(item, "priceInr");
        List<String> vibeTags = getStringList(item, "vibeTags");

        if (imageUrl != null && imageUrl.startsWith("//")) {
            imageUrl = "https:" + imageUrl;
        }

        return CuratedProduct.builder()
                .productName(productName)
                .brandName(brandName)
                .brandLogo(brandLogo)
                .category(category)
                .subcategory(productType)
                .vibeTags(vibeTags)
                .images(imageUrl != null ? List.of(imageUrl) : new ArrayList<>())
                .primaryImageUrl(imageUrl)
                .price(priceInr)
                .description(description)
                .shopUrl(websiteLink)
                .platform("brand")
                .featured(featured != null && featured)
                .active(true)
                .addedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // SAFE TYPE HELPERS
    // ─────────────────────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            log.warn("[SEEDER] Could not parse '{}' as Double for '{}'", val, key);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return new ArrayList<>();
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object i : list) {
                if (i != null) result.add(i.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }
}