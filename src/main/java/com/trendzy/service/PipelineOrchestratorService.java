package com.trendzy.service;

import com.trendzy.config.GroqConfig;
import com.trendzy.model.mongo.Signal;
import com.trendzy.repository.mongo.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineOrchestratorService {

    private final RedditCollectorService redditCollectorService;
    private final YouTubeCollectorService youtubeCollectorService;
    private final AiAnalysisService aiAnalysisService;
    private final ProductEnrichmentService productEnrichmentService;
    private final SignalRepository signalRepository;
    private final GroqConfig groqConfig;

    // ─────────────────────────────────────────────────────────────
    // COLLECTION
    // ─────────────────────────────────────────────────────────────

    @Scheduled(cron = "${scheduler.collection.cron}")
    public Map<String, Integer> triggerCollection() {
        log.info("[PIPELINE] Starting signal collection...");

        int redditCount  = redditCollectorService.collectSignals();
        int youtubeCount = youtubeCollectorService.collectSignals();
        int total        = redditCount + youtubeCount;

        log.info("[PIPELINE] Collection complete — Reddit: {}, YouTube: {}, Total: {}",
                redditCount, youtubeCount, total);

        return Map.of(
                "redditSignals",  redditCount,
                "youtubeSignals", youtubeCount,
                "total",          total
        );
    }

    // ─────────────────────────────────────────────────────────────
    // ANALYSIS
    // ─────────────────────────────────────────────────────────────

    @Scheduled(cron = "${scheduler.analysis.cron}")
    public Map<String, Object> triggerAnalysis() {
        log.info("[PIPELINE] Starting AI analysis...");

        // Prefer signals with buy intent or product keywords (priorityScore)
        List<Signal> unprocessed = signalRepository
                .findByProcessedFalseOrderByPriorityScoreDescCollectedAtDesc(
                        PageRequest.of(0, 100)
                );

        if (unprocessed.isEmpty()) {
            log.info("[PIPELINE] No unprocessed signals found — skipping analysis");
            return Map.of(
                    "totalSignals",     0,
                    "batchesProcessed", 0,
                    "batchesFailed",    0
            );
        }

        log.info("[PIPELINE] Found {} unprocessed signals — starting batched analysis",
                unprocessed.size());

        // Use batch size from config (default 5)
        int batchSize       = groqConfig.getBatchSize() > 0 ? groqConfig.getBatchSize() : 5;
        long rateLimitPause = groqConfig.getRateLimitPauseMs() > 0
                ? groqConfig.getRateLimitPauseMs() : 15000;

        int batchesProcessed = 0;
        int batchesFailed    = 0;
        int totalBatches     = (int) Math.ceil(unprocessed.size() / (double) batchSize);

        for (int i = 0; i < unprocessed.size(); i += batchSize) {
            List<Signal> batch = unprocessed.subList(
                    i, Math.min(i + batchSize, unprocessed.size())
            );

            int batchNumber = (i / batchSize) + 1;
            log.info("[PIPELINE] Processing batch {}/{} ({} signals)",
                    batchNumber, totalBatches, batch.size());

            try {
                aiAnalysisService.analyzeSignalsBatch(batch);
                batchesProcessed++;
                log.info("[PIPELINE] ✅ Batch {}/{} complete", batchNumber, totalBatches);

                // Pause between batches to respect Groq rate limits
                if (i + batchSize < unprocessed.size()) {
                    log.debug("[PIPELINE] Pausing {}ms before next batch...", rateLimitPause);
                    Thread.sleep(rateLimitPause);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[PIPELINE] Analysis interrupted at batch {}", batchNumber);
                break;
            } catch (Exception e) {
                batchesFailed++;
                log.error("[PIPELINE] ❌ Batch {}/{} failed: {}",
                        batchNumber, totalBatches, e.getMessage());
            }
        }

        log.info("[PIPELINE] Analysis complete — batches processed: {}, failed: {}, total signals: {}",
                batchesProcessed, batchesFailed, unprocessed.size());

        return Map.of(
                "totalSignals",     unprocessed.size(),
                "batchesProcessed", batchesProcessed,
                "batchesFailed",    batchesFailed
        );
    }

    // ─────────────────────────────────────────────────────────────
    // ENRICHMENT
    // ─────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 6 * * *")
    public Map<String, Integer> triggerEnrichment() {
        log.info("[PIPELINE] Starting product enrichment...");

        int enriched = 0;
        int failed   = 0;

        try {
            enriched = productEnrichmentService.enrichProductsBatch();
        } catch (Exception e) {
            failed++;
            log.error("[PIPELINE] Enrichment failed: {}", e.getMessage());
        }

        log.info("[PIPELINE] Enrichment complete — enriched: {}, failed: {}",
                enriched, failed);

        return Map.of(
                "productsEnriched", enriched,
                "failed",           failed
        );
    }
}