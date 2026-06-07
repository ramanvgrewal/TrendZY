package com.trendzy.ingestion.repository;

import com.trendzy.ingestion.model.Platform;
import com.trendzy.ingestion.model.TrendSignal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for {@link TrendSignal} documents
 * (collection: {@code v2_signals}).
 *
 * <p>Query methods are designed around three access patterns:
 * <ol>
 *   <li><strong>Ingestion pipeline</strong> — find unprocessed signals for Kafka/AI</li>
 *   <li><strong>Deduplication</strong> — check if a source URL was already ingested</li>
 *   <li><strong>Analytics</strong> — count signals per platform and time window</li>
 * </ol>
 */
@Repository
public interface TrendSignalRepository extends MongoRepository<TrendSignal, String> {

    // ─────────────────────────────────────────────────────────────
    // INGESTION PIPELINE
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns signals that have not yet been processed by the AI worker,
     * ordered by engagement score (highest first) then collection time (newest first).
     * Uses the compound index {@code idx_platform_processed}.
     */
    List<TrendSignal> findByProcessedByAiFalseOrderByEngagementScoreDescCollectedAtDesc(Pageable pageable);

    /**
     * Returns unprocessed signals for a specific platform.
     * Useful when the AI worker wants to batch-process by platform.
     */
    List<TrendSignal> findByPlatformAndProcessedByAiFalseOrderByCollectedAtDesc(
            Platform platform, Pageable pageable);

    // ─────────────────────────────────────────────────────────────
    // DEDUPLICATION
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks whether a signal with the given source URL already exists.
     * Called by the ingestion service before persisting a new signal.
     */
    boolean existsBySourceUrl(String sourceUrl);

    // ─────────────────────────────────────────────────────────────
    // ANALYTICS / QUERYING
    // ─────────────────────────────────────────────────────────────

    /** All signals from a specific platform, paginated. */
    List<TrendSignal> findByPlatform(Platform platform, Pageable pageable);

    /** Count signals collected from a platform within a time window. */
    long countByPlatformAndCollectedAtAfter(Platform platform, Instant since);

    /** Total signals collected across all platforms since a given instant. */
    long countByCollectedAtAfter(Instant since);

    /** Count unprocessed signals — useful for monitoring pipeline backlog. */
    long countByProcessedByAiFalse();
}
