package com.trendzy.ingestion.repository;

import com.trendzy.ingestion.model.Brand;
import com.trendzy.ingestion.model.Platform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link Brand} documents
 * (collection: {@code brands}).
 */
@Repository
public interface BrandRepository extends MongoRepository<Brand, String> {

    // ─────────────────────────────────────────────────────────────
    // LOOKUP
    // ─────────────────────────────────────────────────────────────

    /** Find a brand by its URL-safe slug (e.g. "bewakoof"). */
    Optional<Brand> findBySlug(String slug);

    /** Find a brand by its exact profile URL. */
    Optional<Brand> findByProfileUrl(String profileUrl);

    // ─────────────────────────────────────────────────────────────
    // DEDUPLICATION
    // ─────────────────────────────────────────────────────────────

    /** Check if a brand with this profile URL already exists. */
    boolean existsByProfileUrl(String profileUrl);

    /** Check if a brand with this slug already exists. */
    boolean existsBySlug(String slug);

    // ─────────────────────────────────────────────────────────────
    // PLATFORM-SPECIFIC
    // ─────────────────────────────────────────────────────────────

    /** All active brands discovered on a specific platform. */
    List<Brand> findByPlatformAndActiveTrue(Platform platform);

    /** Paginated active brands for a specific platform. */
    Page<Brand> findByPlatformAndActiveTrue(Platform platform, Pageable pageable);

    // ─────────────────────────────────────────────────────────────
    // GENERAL
    // ─────────────────────────────────────────────────────────────

    /** All active brands, paginated. */
    Page<Brand> findByActiveTrue(Pageable pageable);

    /** Count all active brands. */
    long countByActiveTrue();

    /** Search brands by name (case-insensitive regex). */
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'active': true }")
    List<Brand> findByNameRegexAndActiveTrue(String nameRegex);
}
