package com.trendzy.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "token_usage")
public class TokenUsage {
    @Id
    private String id;
    
    private LocalDate date;
    private int tokensUsed;
    private int tokensRemaining;
    
    private int batchesProcessed;
    private int batchesFailed;
    
    private LocalDateTime resetDate;
}
