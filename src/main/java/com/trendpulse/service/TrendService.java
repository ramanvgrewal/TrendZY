package com.trendpulse.service;

import com.trendpulse.dto.TrendResponse;
import com.trendpulse.entity.Trend;
import com.trendpulse.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendService {

    private final TrendRepository trendRepository;
    private final PersonalisationService personalisationService;

    /**
     * Get all trends with optional filters (R3 + R5 support).
     */
    public List<TrendResponse> getAllTrends(String category, String audience,
            String gender, String pricePoint,
            Boolean indiaRelevant, String sessionId) {
        List<Trend> trends;

        // Apply filter priority: indiaRelevant > gender > pricePoint > category >
        // audience > all
        if (indiaRelevant != null && indiaRelevant) {
            trends = trendRepository.findIndiaRelevant();
        } else if (gender != null && !gender.isBlank()) {
            trends = trendRepository.findByGenderOrderByScoreDesc(gender);
        } else if (pricePoint != null && !pricePoint.isBlank()) {
            trends = trendRepository.findByPricePointOrderByScoreDesc(pricePoint);
        } else if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            trends = trendRepository.findByCategoryOrderByScoreDesc(category);
        } else if (audience != null && !audience.isBlank() && !audience.equalsIgnoreCase("all")) {
            trends = trendRepository.findByAudienceOrderByScoreDesc(audience);
        } else {
            trends = trendRepository.findAllOrderByScoreDesc();
        }

        // R5: Apply personalisation if sessionId is provided
        if (sessionId != null && !sessionId.isBlank()) {
            trends = personalisationService.getPersonalisedFeed(sessionId, trends);
        }

        return trends.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get rising/early-stage trends
     */
    public List<TrendResponse> getRisingTrends() {
        return trendRepository.findRisingTrends()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get single trend by ID with full details
     */
    public Optional<TrendResponse> getTrendById(Long id) {
        return trendRepository.findById(id)
                .map(this::toResponse);
    }

    /**
     * Get all available categories
     */
    public List<String> getCategories() {
        return trendRepository.findDistinctCategories();
    }

    /**
     * Convert entity to response DTO (includes R3 fields)
     */
    private TrendResponse toResponse(Trend trend) {
        List<TrendResponse.ProductLinkDto> linkDtos = trend.getProductLinks() != null
                ? trend.getProductLinks().stream()
                        .map(pl -> TrendResponse.ProductLinkDto.builder()
                                .id(pl.getId())
                                .platform(pl.getPlatform())
                                .affiliateUrl(pl.getAffiliateUrl())
                                .priceRange(pl.getPriceRange())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return TrendResponse.builder()
                .id(trend.getId())
                .productName(trend.getProductName())
                .category(trend.getCategory())
                .trendScore(trend.getTrendScore())
                .velocity(trend.getVelocity())
                .velocityLabel(trend.getVelocityLabel())
                .confidence(trend.getConfidence())
                .aiExplanation(trend.getAiExplanation())
                .mentionCountThisWeek(trend.getMentionCountThisWeek())
                .mentionCountLastWeek(trend.getMentionCountLastWeek())
                .audienceTag(trend.getAudienceTag())
                .imageUrl(trend.getImageUrl())
                .brandMention(trend.getBrandMention())
                .pricePoint(trend.getPricePoint())
                .gender(trend.getGender())
                .indiaRelevant(trend.getIndiaRelevant())
                .createdAt(trend.getCreatedAt())
                .updatedAt(trend.getUpdatedAt())
                .productLinks(linkDtos)
                .build();
    }
}
