package com.trendzy.service;

import com.trendzy.dto.response.SearchResponse;
import com.trendzy.model.mongo.CuratedProduct;
import com.trendzy.model.mongo.Trend;
import com.trendzy.repository.mongo.CuratedProductRepository;
import com.trendzy.repository.mongo.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final TrendRepository trendRepository;
    private final CuratedProductRepository curatedProductRepository;

    // ─────────────────────────────────────────────────────────────
    // AUTOCOMPLETE
    // ─────────────────────────────────────────────────────────────

    public List<SearchResponse.SearchResult> autocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            log.debug("[SEARCH] Autocomplete query too short — skipping");
            return List.of();
        }

        String trimmed = query.trim();
        log.info("[SEARCH] Autocomplete query: '{}'", trimmed);

        // Case-insensitive contains — not just prefix
        String regex = "(?i).*" + escapeRegex(trimmed) + ".*";

        List<SearchResponse.SearchResult> results = new ArrayList<>();

        try {
            List<Trend> trends = trendRepository.findTop8ByProductNameRegex(regex);
            trends.forEach(t -> results.add(SearchResponse.SearchResult.builder()
                    .id(t.getId())
                    .productName(t.getProductName())
                    .category(t.getCategory())
                    .tier(t.getTier())
                    .imageUrl(t.getImageUrl())
                    .price(t.getEstimatedPrice())
                    .vibeTags(t.getVibeTags())
                    .build()));
        } catch (Exception e) {
            log.warn("[SEARCH] Trend autocomplete failed: {}", e.getMessage());
        }

        try {
            List<CuratedProduct> curated = curatedProductRepository
                    .findTop8ByProductNameRegex(regex);
            curated.forEach(c -> results.add(SearchResponse.SearchResult.builder()
                    .id(c.getId())
                    .productName(c.getProductName())
                    .category(c.getCategory())
                    .tier("curated")
                    .imageUrl(c.getPrimaryImageUrl())
                    .price(c.getPrice())
                    .brandName(c.getBrandName())
                    .vibeTags(c.getVibeTags())
                    .build()));
        } catch (Exception e) {
            log.warn("[SEARCH] Curated autocomplete failed: {}", e.getMessage());
        }

        List<SearchResponse.SearchResult> limited = results.stream()
                .limit(8)
                .collect(Collectors.toList());

        log.info("[SEARCH] Autocomplete '{}' → {} suggestions", trimmed, limited.size());
        return limited;
    }

    // ─────────────────────────────────────────────────────────────
    // FULL SEARCH
    // ─────────────────────────────────────────────────────────────

    public SearchResponse search(String query,
                                 String type,
                                 String vibe,
                                 Pageable pageable) {

        // Null safety
        String safeQuery = query != null ? query.trim() : "";
        String safeType  = type  != null ? type.trim().toLowerCase()  : "all";
        String safeVibe  = vibe  != null ? vibe.trim()  : "All";

        log.info("[SEARCH] query: '{}', type: '{}', vibe: '{}', page: {}",
                safeQuery, safeType, safeVibe, pageable.getPageNumber());

        if (safeQuery.isEmpty()) {
            log.warn("[SEARCH] Empty query — returning empty results");
            return emptyResponse(pageable);
        }

        List<SearchResponse.SearchResult> combined = new ArrayList<>();
        long totalElements = 0;
        int totalPages = 1;

        boolean isVibeFiltered = !safeVibe.equalsIgnoreCase("All")
                && !safeVibe.isBlank();

        // ── Trends (trending + rising) ──────────────────────────
        if (safeType.equals("all") || safeType.equals("trending")
                || safeType.equals("rising")) {
            try {
                Page<Trend> trendPage;

                if (isVibeFiltered && !safeType.equals("all")) {
                    // Filter by keyword + tier + vibe in DB
                    trendPage = trendRepository
                            .searchByKeywordAndTierAndVibe(
                                    safeQuery, safeType, safeVibe, pageable);
                } else if (isVibeFiltered) {
                    // Filter by keyword + vibe in DB
                    trendPage = trendRepository
                            .searchByKeywordAndVibe(safeQuery, safeVibe, pageable);
                } else if (!safeType.equals("all")) {
                    // Filter by keyword + tier in DB
                    trendPage = trendRepository
                            .searchByKeywordAndTier(safeQuery, safeType, pageable);
                } else {
                    // Keyword only
                    trendPage = trendRepository.searchByKeyword(safeQuery, pageable);
                }

                trendPage.forEach(t -> combined.add(
                        SearchResponse.SearchResult.builder()
                                .id(t.getId())
                                .productName(t.getProductName())
                                .category(t.getCategory())
                                .tier(t.getTier())
                                .imageUrl(t.getImageUrl())
                                .price(t.getEstimatedPrice())
                                .amazonUrl(t.getAmazonUrl())
                                .myntraUrl(t.getMyntraUrl())
                                .vibeTags(t.getVibeTags())
                                .build()));

                totalElements += trendPage.getTotalElements();
                totalPages = Math.max(totalPages, trendPage.getTotalPages());

                log.info("[SEARCH] Trends found: {} (total: {})",
                        trendPage.getNumberOfElements(),
                        trendPage.getTotalElements());

            } catch (Exception e) {
                log.error("[SEARCH] Trend search failed: {}", e.getMessage());
            }
        }

        // ── Curated ─────────────────────────────────────────────
        if (safeType.equals("all") || safeType.equals("curated")) {
            try {
                Page<CuratedProduct> curatedPage;

                if (isVibeFiltered) {
                    curatedPage = curatedProductRepository
                            .searchByKeywordAndVibe(safeQuery, safeVibe, pageable);
                } else {
                    curatedPage = curatedProductRepository
                            .searchByKeyword(safeQuery, pageable);
                }

                curatedPage.forEach(c -> combined.add(
                        SearchResponse.SearchResult.builder()
                                .id(c.getId())
                                .productName(c.getProductName())
                                .category(c.getCategory())
                                .tier("curated")
                                .imageUrl(c.getPrimaryImageUrl())
                                .price(c.getPrice())
                                .brandName(c.getBrandName())
                                .shopUrl(c.getShopUrl())
                                .vibeTags(c.getVibeTags())
                                .build()));

                totalElements += curatedPage.getTotalElements();
                totalPages = Math.max(totalPages, curatedPage.getTotalPages());

                log.info("[SEARCH] Curated found: {} (total: {})",
                        curatedPage.getNumberOfElements(),
                        curatedPage.getTotalElements());

            } catch (Exception e) {
                log.error("[SEARCH] Curated search failed: {}", e.getMessage());
            }
        }

        log.info("[SEARCH] Final combined results: {} | totalPages: {}",
                combined.size(), totalPages);

        return SearchResponse.builder()
                .results(combined)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(pageable.getPageNumber())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // TRENDING QUERIES — from real DB data
    // ─────────────────────────────────────────────────────────────

    public List<String> getTrendingQueries() {
        log.info("[SEARCH] Fetching trending queries from DB");
        try {
            // Get top 8 trending product names as query suggestions
            List<Trend> topTrends = trendRepository
                    .findTop8ByTierAndActiveTrueOrderByTrendScoreDesc("trending");

            if (!topTrends.isEmpty()) {
                List<String> queries = topTrends.stream()
                        .map(Trend::getProductName)
                        .collect(Collectors.toList());
                log.info("[SEARCH] Returning {} trending queries from DB", queries.size());
                return queries;
            }
        } catch (Exception e) {
            log.warn("[SEARCH] Could not fetch trending queries from DB: {}", e.getMessage());
        }

        // Fallback only if DB is empty
        log.info("[SEARCH] DB empty — returning fallback trending queries");
        return List.of(
                "baggy jeans", "lip tint", "sunscreen",
                "sneakers", "co-ord set", "mini dress", "tote bag"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private SearchResponse emptyResponse(Pageable pageable) {
        return SearchResponse.builder()
                .results(List.of())
                .totalElements(0)
                .totalPages(0)
                .currentPage(pageable.getPageNumber())
                .build();
    }

    // Escape special regex characters in user input
    private String escapeRegex(String input) {
        return input.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }
}