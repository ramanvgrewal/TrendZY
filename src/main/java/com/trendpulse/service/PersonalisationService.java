package com.trendpulse.service;

import com.trendpulse.entity.Trend;
import com.trendpulse.entity.UserPreference;
import com.trendpulse.repository.TrendRepository;
import com.trendpulse.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * R5: Session-based personalisation service (no login required).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalisationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final TrendRepository trendRepository;

    /**
     * Record that a user viewed a trend.
     */
    public void recordView(String sessionId, Long trendId) {
        UserPreference pref = getOrCreate(sessionId);
        if (!pref.getViewedTrendIds().contains(trendId)) {
            pref.getViewedTrendIds().add(trendId);
        }
        userPreferenceRepository.save(pref);
    }

    /**
     * Record a buy-click — adds to clicked_buy_ids and boosts the trend's category.
     */
    public void recordBuyClick(String sessionId, Long trendId) {
        UserPreference pref = getOrCreate(sessionId);

        if (!pref.getClickedBuyIds().contains(trendId)) {
            pref.getClickedBuyIds().add(trendId);
        }

        // Boost the trend's category in liked_categories
        trendRepository.findById(trendId).ifPresent(trend -> {
            if (trend.getCategory() != null && !pref.getLikedCategories().contains(trend.getCategory())) {
                pref.getLikedCategories().add(trend.getCategory());
            }
            // Infer audience tag from interactions
            if (trend.getAudienceTag() != null) {
                pref.setAudienceTag(trend.getAudienceTag());
            }
        });

        userPreferenceRepository.save(pref);
    }

    /**
     * Get preferences for a session.
     */
    public Optional<UserPreference> getPreferences(String sessionId) {
        return userPreferenceRepository.findBySessionId(sessionId);
    }

    /**
     * Re-rank trends based on user preferences:
     * - Boost trends matching liked_categories
     * - Deprioritize already-viewed trends
     */
    public List<Trend> getPersonalisedFeed(String sessionId, List<Trend> trends) {
        Optional<UserPreference> optPref = userPreferenceRepository.findBySessionId(sessionId);
        if (optPref.isEmpty())
            return trends;

        UserPreference pref = optPref.get();
        Set<String> likedCats = new HashSet<>(
                pref.getLikedCategories() != null ? pref.getLikedCategories() : List.of());
        Set<Long> viewedIds = new HashSet<>(pref.getViewedTrendIds() != null ? pref.getViewedTrendIds() : List.of());

        return trends.stream()
                .sorted((a, b) -> {
                    double scoreA = computePersonalisedScore(a, likedCats, viewedIds);
                    double scoreB = computePersonalisedScore(b, likedCats, viewedIds);
                    return Double.compare(scoreB, scoreA); // descending
                })
                .collect(Collectors.toList());
    }

    private double computePersonalisedScore(Trend trend, Set<String> likedCats, Set<Long> viewedIds) {
        double score = trend.getTrendScore() != null ? trend.getTrendScore() : 0.0;

        // Boost if category matches user preferences (+2)
        if (trend.getCategory() != null && likedCats.contains(trend.getCategory())) {
            score += 2.0;
        }

        // Deprioritize already-viewed trends (-3)
        if (trend.getId() != null && viewedIds.contains(trend.getId())) {
            score -= 3.0;
        }

        return score;
    }

    private UserPreference getOrCreate(String sessionId) {
        return userPreferenceRepository.findBySessionId(sessionId)
                .orElseGet(() -> UserPreference.builder()
                        .sessionId(sessionId)
                        .likedCategories(new ArrayList<>())
                        .viewedTrendIds(new ArrayList<>())
                        .clickedBuyIds(new ArrayList<>())
                        .build());
    }
}
