package com.trendzy.ingestion.repository;

import com.trendzy.ingestion.model.Aesthetic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link Aesthetic} documents
 * (collection: {@code aesthetics}).
 */
@Repository
public interface AestheticRepository extends MongoRepository<Aesthetic, String> {

    // ─────────────────────────────────────────────────────────────
    // LOOKUP
    // ─────────────────────────────────────────────────────────────

    /** Find an aesthetic by its URL-safe slug (e.g. "dark-academia"). */
    Optional<Aesthetic> findBySlug(String slug);

    /** Check existence by slug — used before creating a new aesthetic. */
    boolean existsBySlug(String slug);

    // ─────────────────────────────────────────────────────────────
    // ACTIVE QUERIES
    // ─────────────────────────────────────────────────────────────

    /** All active aesthetics, paginated. */
    Page<Aesthetic> findByActiveTrue(Pageable pageable);

    /** Top N active aesthetics ordered by trend score descending. */
    List<Aesthetic> findByActiveTrueOrderByTrendScoreDesc(Pageable pageable);

    /** Count active aesthetics. */
    long countByActiveTrue();

    // ─────────────────────────────────────────────────────────────
    // HASHTAG SEARCH
    // ─────────────────────────────────────────────────────────────

    /**
     * Find aesthetics whose {@code relatedHashtags} array contains the given hashtag.
     * Useful for mapping a scraped hashtag to an existing aesthetic.
     */
    @Query("{ 'relatedHashtags': ?0, 'active': true }")
    List<Aesthetic> findByRelatedHashtagAndActiveTrue(String hashtag);
}
