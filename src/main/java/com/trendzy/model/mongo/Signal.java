package com.trendzy.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "signals")
public class Signal {
    @Id
    private String id;
    
    private String source; // "reddit" | "youtube"
    private String sourceId;
    
    private String subreddit;
    private String content;
    private String url;
    private long upvotes;
    private long commentCount;
    
    private boolean processed;
    private List<String> buyIntentKeywords;
    /** Higher = prefer for analysis (buy intent or product keywords present). */
    private int priorityScore;

    private LocalDateTime collectedAt;
}
