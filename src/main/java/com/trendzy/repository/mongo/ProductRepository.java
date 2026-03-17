package com.trendzy.repository.mongo;

import com.trendzy.model.mongo.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByTrendId(String trendId);
    List<Product> findByTrendIdIn(List<String> trendIds);
}
