package com.trendpulse.repository;

import com.trendpulse.entity.Trend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrendRepository extends JpaRepository<Trend, Long> {

    List<Trend> findByCategoryIgnoreCase(String category);

    List<Trend> findByAudienceTagIgnoreCase(String audienceTag);

    List<Trend> findByVelocityLabel(String velocityLabel);

    @Query("SELECT t FROM Trend t ORDER BY t.trendScore DESC")
    List<Trend> findAllOrderByScoreDesc();

    @Query("SELECT t FROM Trend t WHERE t.velocityLabel = 'Rising Fast' OR (t.velocity > 50 AND t.trendScore < 8) ORDER BY t.velocity DESC")
    List<Trend> findRisingTrends();

    @Query("SELECT t FROM Trend t WHERE t.category = :category ORDER BY t.trendScore DESC")
    List<Trend> findByCategoryOrderByScoreDesc(String category);

    @Query("SELECT t FROM Trend t WHERE t.audienceTag = :audienceTag ORDER BY t.trendScore DESC")
    List<Trend> findByAudienceOrderByScoreDesc(String audienceTag);

    @Query("SELECT DISTINCT t.category FROM Trend t WHERE t.category IS NOT NULL ORDER BY t.category")
    List<String> findDistinctCategories();

    Optional<Trend> findByProductNameIgnoreCase(String productName);

    // R3: New filter queries
    @Query("SELECT t FROM Trend t WHERE t.indiaRelevant = true ORDER BY t.trendScore DESC")
    List<Trend> findIndiaRelevant();

    @Query("SELECT t FROM Trend t WHERE t.gender = :gender ORDER BY t.trendScore DESC")
    List<Trend> findByGenderOrderByScoreDesc(String gender);

    @Query("SELECT t FROM Trend t WHERE t.pricePoint = :pricePoint ORDER BY t.trendScore DESC")
    List<Trend> findByPricePointOrderByScoreDesc(String pricePoint);

    long countByIndiaRelevantTrue();
}
