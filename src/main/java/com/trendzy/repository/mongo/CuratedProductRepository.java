package com.trendzy.repository.mongo;

import com.trendzy.model.mongo.CuratedProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuratedProductRepository extends MongoRepository<CuratedProduct, String> {

    Page<CuratedProduct> findByActiveTrue(Pageable pageable);
    List<CuratedProduct> findByActiveTrue();

    @Query("{ 'vibeTags': ?0, 'active': true }")
    Page<CuratedProduct> findByVibeTagAndActiveTrue(String vibeTag, Pageable pageable);

    @Query("{ 'brandName': ?0, 'active': true }")
    Page<CuratedProduct> findByBrandNameAndActiveTrue(String brandName, Pageable pageable);

    Page<CuratedProduct> findByBrandNameAndVibeTagsContainingAndActiveTrue(
            String brandName, String vibeTag, Pageable pageable);

    Page<CuratedProduct> findByVibeTagsContainingAndActiveTrue(
            String vibeTag, Pageable pageable);

    // Change findFirst... to findAll...
    List<CuratedProduct> findAllByFeaturedTrueAndActiveTrue();

    // In CuratedProductRepository.java
    List<CuratedProduct> findAllByActiveTrue();

    boolean existsByProductNameAndBrandName(String productName, String brandName);

    // ── Autocomplete ───────────────────────────────────────────
    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'active': true }")
    List<CuratedProduct> findTop8ByProductNameRegex(String regex);

    // ── Full text search ───────────────────────────────────────
    // ── CuratedProductRepository — replace all $text queries ─────

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'active': true }")
    Page<CuratedProduct> searchByKeyword(String keyword, Pageable pageable);

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'vibeTags': ?1, 'active': true }")
    Page<CuratedProduct> searchByKeywordAndVibe(String keyword, String vibe, Pageable pageable);

    // Change existsBy to findBy so we can get the actual object to update
    Optional<CuratedProduct> findByProductNameAndBrandName(
            String productName, String brandName);
}