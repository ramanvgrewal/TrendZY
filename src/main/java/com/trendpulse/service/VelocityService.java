package com.trendpulse.service;

import com.trendpulse.entity.Trend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VelocityService {

    /**
     * Calculate velocity percentage and assign velocity label to a trend.
     *
     * Velocity labels:
     * - "Rising Fast" → >200% week-over-week increase
     * - "Trending Now" → 50–200% increase
     * - "Underrated Gem" → <50% increase but score > 7
     * - "Emerging" → new trend with no prior data
     * - "Stable" → everything else
     */
    public void calculateAndApplyVelocity(Trend trend) {
        int thisWeek = trend.getMentionCountThisWeek();
        int lastWeek = trend.getMentionCountLastWeek();

        double velocityPct;

        if (lastWeek == 0) {
            // New trend — assign velocity based on current mentions
            velocityPct = thisWeek > 5 ? 300.0 : thisWeek * 50.0;
        } else {
            velocityPct = ((double) (thisWeek - lastWeek) / lastWeek) * 100.0;
        }

        trend.setVelocity(velocityPct);

        // Assign velocity label
        String label;
        if (velocityPct > 200) {
            label = "Rising Fast";
        } else if (velocityPct >= 50) {
            label = "Trending Now";
        } else if (trend.getTrendScore() != null && trend.getTrendScore() > 7 && velocityPct < 50) {
            label = "Underrated Gem";
        } else if (lastWeek == 0 && thisWeek > 0) {
            label = "Emerging";
        } else {
            label = "Stable";
        }

        trend.setVelocityLabel(label);
        log.debug("Velocity for '{}': {}% → {}", trend.getProductName(), String.format("%.1f", velocityPct), label);
    }

    /**
     * Weekly rollover: shift this week's count to last week and reset current.
     * Should be called by a weekly scheduled job.
     */
    public void weeklyRollover(Trend trend) {
        trend.setMentionCountLastWeek(trend.getMentionCountThisWeek());
        trend.setMentionCountThisWeek(0);
    }
}
