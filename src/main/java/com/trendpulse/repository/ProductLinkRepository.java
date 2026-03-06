package com.trendpulse.repository;

import com.trendpulse.entity.ProductLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductLinkRepository extends JpaRepository<ProductLink, Long> {

    List<ProductLink> findByTrendId(Long trendId);

    List<ProductLink> findByTrendIdAndPlatform(Long trendId, String platform);

    void deleteByTrendId(Long trendId);
}
