package com.trendzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendStatsResponse {
    private long signalsToday;
    private long trendingCount;
    private long risingCount;
    private int subredditsMonitored;
    private LocalDateTime lastCollectionTime;
    
    private int groqTokensUsedToday;
    private int groqTokensRemaining;
}
