package com.trendzy.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "click_events")
public class ClickEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String productId;
    
    private String productType; // "trend" | "curated"
    
    private String platform;
    
    private String source; // page name
    
    private Long userId; // nullable
    
    private String ipAddress;
    
    private LocalDateTime clickedAt;
}
