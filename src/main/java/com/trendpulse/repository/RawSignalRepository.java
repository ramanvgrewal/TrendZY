package com.trendpulse.repository;

import com.trendpulse.entity.RawSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RawSignalRepository extends JpaRepository<RawSignal, Long> {

    List<RawSignal> findByProcessedFalse();

    List<RawSignal> findBySubreddit(String subreddit);

    List<RawSignal> findByProductMentionedIgnoreCase(String productMentioned);

    @Query("SELECT r FROM RawSignal r WHERE r.processed = false ORDER BY r.collectedAt DESC")
    List<RawSignal> findUnprocessedSignals();

    @Query("SELECT COUNT(r) FROM RawSignal r WHERE r.productMentioned = :product AND r.collectedAt >= :since")
    long countMentionsSince(String product, LocalDateTime since);

    boolean existsByPostIdAndSubreddit(String postId, String subreddit);
}
