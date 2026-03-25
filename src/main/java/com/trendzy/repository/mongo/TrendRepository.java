package com.trendzy.repository.mongo;

import com.trendzy.model.mongo.Trend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrendRepository extends MongoRepository<Trend, String> {

    Page<Trend> findByTierAndActiveTrue(String tier, Pageable pageable);

    @Query("{ 'tier': ?0, 'vibeTags': ?1, 'active': true }")
    Page<Trend> findByTierAndVibeTagAndActiveTrue(String tier, String vibeTag, Pageable pageable);

    @Query("{ 'vibeTags': ?0, 'active': true }")
    Page<Trend> findByVibeTagAndActiveTrue(String vibeTag, Pageable pageable);

    boolean existsByProductNameIgnoreCase(String productName);

    @Query(value = "{ 'active': true }", sort = "{ 'trendScore': -1 }")
    List<Trend> findTopTrends(Pageable pageable);

    long countByTier(String tier);
    long countByActiveTrue();

    List<Trend> findByCategoryAndIdNotAndActiveTrue(String category, String id, Pageable pageable);

    List<Trend> findByImageUrlIsNullAndActiveTrue();

    @Query("{ '$or': [{'enrichmentStatus': 'PENDING'}, {'enrichmentStatus': null}], 'active': true }")
    List<Trend> findPendingEnrichment();

    List<Trend> findTop8ByTierAndActiveTrueOrderByTrendScoreDesc(String tier);

    // For findAll with active filter
    Page<Trend> findByActiveTrue(Pageable pageable);

    // ── Autocomplete ───────────────────────────────────────────
    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'active': true }")
    List<Trend> findTop8ByProductNameRegex(String regex);

    // ── Full text search — requires text index on productName + category ──
    // ── TrendRepository — replace all $text queries ──────────────

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'active': true }")
    Page<Trend> searchByKeyword(String keyword, Pageable pageable);

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'tier': ?1, 'active': true }")
    Page<Trend> searchByKeywordAndTier(String keyword, String tier, Pageable pageable);

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'vibeTags': ?1, 'active': true }")
    Page<Trend> searchByKeywordAndVibe(String keyword, String vibe, Pageable pageable);

    @Query("{ 'productName': { $regex: ?0, $options: 'i' }, 'tier': ?1, 'vibeTags': ?2, 'active': true }")
    Page<Trend> searchByKeywordAndTierAndVibe(String keyword, String tier,
                                              String vibe, Pageable pageable);
}