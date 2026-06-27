package com.trendzy.ingestion.scraper;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.trendzy.ingestion.scraper.instagram.InstagramSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramBioExtractor {

    private final InstagramSessionManager sessionManager;

    // ── Pre-compiled Regex Patterns ──
    private static final Pattern EXTERNAL_URL_PATTERN =
            Pattern.compile("\"external_url\"\\s*:\\s*\"(https?[^\"]+)\"");

    private static final Pattern AGGREGATOR_PATTERN =
            Pattern.compile("(https?://(?:linktr\\.ee|beacons\\.ai|solo\\.to|bio\\.site|bio\\.link|campsite\\.bio|hoo\\.be|linkpop\\.com)[^\\s\"'<>]+)");

    private static final Pattern BIO_LINK_PATTERN =
            Pattern.compile("\"bio_links\"\\s*:\\s*\\[\\s*\\{[^}]*\"url\"\\s*:\\s*\"(https?[^\"]+)\"");

    private static final Pattern LINK_HEADER_PATTERN =
            Pattern.compile("\"link_header\"\\s*:\\s*\"(https?[^\"]+)\"");

    public String extractBioLink(String username, BrowserContext context) {
        if (username == null || username.isBlank()) return null;

        String profileUrl = "https://www.instagram.com/" + username + "/";
        log.info("[INSTAGRAM] Extracting bio link for @{}", username);

        try (Page page = context.newPage()) {
            // Navigate and immediately grab raw HTML — do NOT wait for full DOM hydration
            page.navigate(profileUrl);
            page.waitForTimeout(3000); // Minimal wait for raw HTML to arrive

            String html = page.content();
            if (html == null || html.isBlank()) {
                log.warn("[INSTAGRAM] Empty page content for @{}", username);
                return null;
            }

            // ── Strategy 1: Extract external_url from embedded JSON ──
            try {
                Matcher matcher = EXTERNAL_URL_PATTERN.matcher(html);
                if (matcher.find()) {
                    String link = unescapeUrl(matcher.group(1));
                    if (isValidBioLink(link)) {
                        log.info("[INSTAGRAM] Found bio link via external_url JSON for @{}: {}", username, link);
                        return link;
                    }
                }
            } catch (Exception e) {
                log.warn("[INSTAGRAM] external_url regex failed for @{}: {}", username, e.getMessage());
            }

            // ── Strategy 2: Extract from bio_links JSON array ──
            try {
                Matcher matcher = BIO_LINK_PATTERN.matcher(html);
                if (matcher.find()) {
                    String link = unescapeUrl(matcher.group(1));
                    if (isValidBioLink(link)) {
                        log.info("[INSTAGRAM] Found bio link via bio_links JSON for @{}: {}", username, link);
                        return link;
                    }
                }
            } catch (Exception e) {
                log.warn("[INSTAGRAM] bio_links regex failed for @{}: {}", username, e.getMessage());
            }

            // ── Strategy 3: Extract from link_header JSON field ──
            try {
                Matcher matcher = LINK_HEADER_PATTERN.matcher(html);
                if (matcher.find()) {
                    String link = unescapeUrl(matcher.group(1));
                    if (isValidBioLink(link)) {
                        log.info("[INSTAGRAM] Found bio link via link_header JSON for @{}: {}", username, link);
                        return link;
                    }
                }
            } catch (Exception e) {
                log.warn("[INSTAGRAM] link_header regex failed for @{}: {}", username, e.getMessage());
            }

            // ── Strategy 4: Scan raw HTML for known aggregator URLs ──
            try {
                Matcher matcher = AGGREGATOR_PATTERN.matcher(html);
                if (matcher.find()) {
                    String link = unescapeUrl(matcher.group(1));
                    log.info("[INSTAGRAM] Found bio link via aggregator pattern for @{}: {}", username, link);
                    return link;
                }
            } catch (Exception e) {
                log.warn("[INSTAGRAM] Aggregator regex failed for @{}: {}", username, e.getMessage());
            }

            // ── Strategy 5: Last-resort DOM extraction (may fail behind login wall) ──
            try {
                var links = page.querySelectorAll("header a[target='_blank']");
                for (var linkEl : links) {
                    String href = linkEl.getAttribute("href");
                    if (href != null && href.startsWith("http") && !href.contains("instagram.com")) {
                        log.info("[INSTAGRAM] Found bio link via DOM fallback for @{}: {}", username, href);
                        return href;
                    }
                }
            } catch (Exception e) {
                log.warn("[INSTAGRAM] DOM fallback failed for @{}: {}", username, e.getMessage());
            }

        } catch (Exception e) {
            log.error("[INSTAGRAM] Fatal error extracting bio link for @{}: {}", username, e.getMessage());
        }

        log.debug("[INSTAGRAM] No bio link found for @{}", username);
        return null;
    }

    private String unescapeUrl(String url) {
        if (url == null) return null;
        return url
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u003d", "=");
    }

    private boolean isValidBioLink(String url) {
        if (url == null || url.isBlank()) return false;
        if (url.contains("instagram.com")) return false;
        if (url.contains("facebook.com")) return false;
        return url.startsWith("http://") || url.startsWith("https://");
    }
}