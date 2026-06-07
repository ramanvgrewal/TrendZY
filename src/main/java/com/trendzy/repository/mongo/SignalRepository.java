package com.trendzy.repository.mongo;

import com.trendzy.model.mongo.Signal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<Signal, String> {
    boolean existsBySourceId(String sourceId);
    long countByCollectedAtAfter(LocalDateTime dateTime);

    List<Signal> findByProcessedFalseOrderByPriorityScoreDescCollectedAtDesc(Pageable pageable);
}
