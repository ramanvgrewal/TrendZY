package com.trendzy.ingestion.repository;

import com.trendzy.ingestion.model.Trend;
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

    // ── India Relevant ──────────────────────────────────────────
    Page<Trend> findByIndiaRelevantAndActiveTrue(boolean indiaRelevant, Pageable pageable);

    Page<Trend> findByTierAndIndiaRelevantAndActiveTrue(String tier, boolean indiaRelevant, Pageable pageable);

    @Query("{ 'tier': ?0, 'vibeTags': ?1, 'indiaRelevant': ?2, 'active': true }")
    Page<Trend> findByTierAndVibeTagAndIndiaRelevantAndActiveTrue(String tier, String vibeTag, boolean indiaRelevant, Pageable pageable);

    @Query("{ 'vibeTags': ?0, 'indiaRelevant': ?1, 'active': true }")
    Page<Trend> findByVibeTagAndIndiaRelevantAndActiveTrue(String vibeTag, boolean indiaRelevant, Pageable pageable);

    // Swapped productName for trendName
    boolean existsByTrendNameIgnoreCase(String trendName);

    @Query(value = "{ 'active': true }", sort = "{ 'trendScore': -1 }")
    List<Trend> findTopTrends(Pageable pageable);

    long countByTier(String tier);
    long countByActiveTrue();
    long countByIndiaRelevantTrueAndActiveTrue();

    // Swapped category for aestheticId for fetching related trends
    List<Trend> findByAestheticIdAndIdNotAndActiveTrue(String aestheticId, String id, Pageable pageable);

    @Query("{ '$or': [{'enrichmentStatus': 'PENDING'}, {'enrichmentStatus': null}], 'active': true }")
    List<Trend> findPendingEnrichment();

    List<Trend> findTop8ByTierAndActiveTrueOrderByTrendScoreDesc(String tier);

    // For findAll with active filter
    Page<Trend> findByActiveTrue(Pageable pageable);

    // ── Autocomplete ───────────────────────────────────────────
    // Swapped productName for trendName
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'active': true }")
    List<Trend> findTop8ByTrendNameRegex(String regex);

    // ── Full text search — Swapped productName for trendName ──

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'active': true }")
    Page<Trend> searchByKeyword(String keyword, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'tier': ?1, 'active': true }")
    Page<Trend> searchByKeywordAndTier(String keyword, String tier, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'vibeTags': ?1, 'active': true }")
    Page<Trend> searchByKeywordAndVibe(String keyword, String vibe, Pageable pageable);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'tier': ?1, 'vibeTags': ?2, 'active': true }")
    Page<Trend> searchByKeywordAndTierAndVibe(String keyword, String tier, String vibe, Pageable pageable);
}