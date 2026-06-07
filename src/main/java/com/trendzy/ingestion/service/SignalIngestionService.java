package com.trendzy.ingestion.service;

import com.microsoft.playwright.Playwright;
import com.trendzy.ingestion.model.TrendSignal;
import com.trendzy.ingestion.repository.TrendSignalRepository;
import com.trendzy.ingestion.scraper.instagram.InstagramExploreClient;
import com.trendzy.ingestion.kafka.KafkaSignalProducer;
import com.trendzy.ingestion.scraper.pinterest.PinterestExploreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the V2 data ingestion cycle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SignalIngestionService {

    private final InstagramExploreClient instagramExploreClient;
    private final PinterestExploreClient pinterestExploreClient;
    private final TrendSignalRepository trendSignalRepository;
    private final KafkaSignalProducer kafkaSignalProducer;

    @Async
    public void runIngestionCycle(String instagramSection, String pinterestQuery) {
        log.info("[INGESTION] ════════ STARTING V2 INGESTION CYCLE ════════");
        
        List<TrendSignal> allSignals = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            // Instagram Extraction
            try {
                allSignals.addAll(instagramExploreClient.fetchExploreSignals(playwright, instagramSection));
            } catch (Exception e) {
                log.error("[INGESTION] Instagram scraper failed: {}", e.getMessage());
            }

            // Pinterest Extraction
            try {
                allSignals.addAll(pinterestExploreClient.fetchExploreSignals(playwright, pinterestQuery));
            } catch (Exception e) {
                log.error("[INGESTION] Pinterest scraper failed: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("[INGESTION] Fatal Playwright error: {}", e.getMessage(), e);
            return;
        }

        int newSignals = 0;
        for (TrendSignal signal : allSignals) {
            if (!trendSignalRepository.existsBySourceUrl(signal.getSourceUrl())) {
                try {
                    TrendSignal saved = trendSignalRepository.save(signal);
                    kafkaSignalProducer.publishSignalEvent(saved.getId(), saved.getPlatform());
                    newSignals++;
                } catch (Exception e) {
                    log.error("[INGESTION] Failed to process signal {}: {}", signal.getSourceUrl(), e.getMessage());
                }
            }
        }
        log.info("[INGESTION] CYCLE COMPLETE | New Signals: {} | Total Scraped: {}", newSignals, allSignals.size());
    }
}
