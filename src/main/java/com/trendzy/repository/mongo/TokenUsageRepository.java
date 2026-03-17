package com.trendzy.repository.mongo;

import com.trendzy.model.mongo.TokenUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TokenUsageRepository extends MongoRepository<TokenUsage, String> {
    Optional<TokenUsage> findByDate(LocalDate date);
}
