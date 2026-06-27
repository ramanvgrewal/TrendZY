package com.trendzy.ingestion.repository;

import com.trendzy.ingestion.model.TokenUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenUsageRepository extends MongoRepository<TokenUsage, String> {

    // Fixed: Matches the 'timestamp' field in TokenUsage.java exactly
    Optional<TokenUsage> findByTimestamp(Instant timestamp);
}