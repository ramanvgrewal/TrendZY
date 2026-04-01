package com.trendzy.scraper.instagram;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.trendzy.scraper.dto.BrandDto;
import com.trendzy.scraper.util.RandomDelayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts brand profile data (username, bio, website link) from Instagram post/reel URLs.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstagramProfileClient {

    private final InstagramSessionManager sessionManager;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int PAGE_TIMEOUT_MS = 8_000;

    private static final List<String> LINK_SELECTORS = List.of(
            "a[href*='l.instagram.com']",
            "a[href*='http'][rel~='nofollow']",
            "a[href*='http'][class*='x1i10hfl']",
            "a[href*='http'][class*='notranslate']",
            "a[href*='linktree']",
            "a[href*='beacons.ai']",
            "a[href*='bio.site']",
            "a[href*='link.bio']"
    );

    private static final Pattern REJECT_URL = Pattern.compile(
            "(facebook\\.com|twitter\\.com|x\\.com|youtube\\.com|tiktok\\.com|javascript|amazon\\.|flipkart\\.|myntra\\.|ajio\\.|meesho\\.|snapdeal\\.|nykaa\\.)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> REJECTED_BRAND_NAMES = List.of(
            "gucci", "louis vuitton", "prada", "chanel", "burberry", "hermes",
            "pantaloons", "shoppersstop", "lifestyle", "allensolly", "vanheusen", "peterengland", "raymond"
    );

    private static final Pattern TEXT_URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    );

    private static final List<String> RESERVED_PATHS = List.of(
            "explore", "p", "reel", "stories", "accounts", "tags", "direct"
    );

    public List<BrandDto> extractBrandsFromPosts(List<String> postUrls, Playwright playwright) {
        Map<String, BrandDto> brandMap = new LinkedHashMap<>();

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = createContext(browser);
            Page page = context.newPage();

            int processed = 0;
            for (String postUrl : postUrls) {
                processed++;
                log.info("[PROFILE] Processing post {}/{}: {}", processed, postUrls.size(), postUrl);
                try {
                    BrandDto brand = extractBrandFromPost(page, postUrl);
                    if (brand != null && !brandMap.containsKey(brand.getUsername())) {
                        brandMap.put(brand.getUsername(), brand);
                        log.info("[PROFILE] Brand extracted: @{} -> {}", brand.getUsername(), brand.getWebsiteUrl());
                    }
                } catch (Exception e) {
                    log.warn("[PROFILE] Skipping post {}: {}", postUrl, e.getMessage());
                }
                RandomDelayUtil.delay();

                if (processed % 8 == 0) {
                    log.info("[PROFILE] Processed {} posts — pausing 15s to avoid rate limiting", processed);
                    RandomDelayUtil.delay(15_000, 20_000, "rate-limit-pause");
                }
            }
            context.close();
        } catch (Exception e) {
            log.error("[PROFILE] Browser error: {}", e.getMessage(), e);
        }

        List<BrandDto> brands = new ArrayList<>(brandMap.values());
        log.info("[PROFILE] Total brands with websites: {}", brands.size());
        return brands;
    }

    private BrandDto extractBrandFromPost(Page page, String postUrl) {
        log.debug("[PROFILE] Opening post: {}", postUrl);
        page.navigate(postUrl);

        try {
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(PAGE_TIMEOUT_MS));
        } catch (TimeoutError te) {
            log.debug("[PROFILE] DOM timeout for post: {}", postUrl);
        }

        log.info("[PROFILE-DEBUG] Post URL: {}", postUrl);
        log.info("[PROFILE-DEBUG] Page URL after navigate: {}", page.url());
        log.info("[PROFILE-DEBUG] Page title: {}", page.title());

        // Check if session expired
        if (isLoginPage(page.url())) {
            log.error("[PROFILE-DEBUG] *** SESSION EXPIRED — login page detected ***");
            sessionManager.invalidateSession();
            return null;
        }

        // Check if Instagram is blocking us
        String pageTitle = page.title();
        if (pageTitle != null && (pageTitle.contains("Log in") || pageTitle.contains("Sign up")
                || pageTitle.contains("Instagram"))) {
            log.warn("[PROFILE-DEBUG] Possible block/redirect — title is: {}", pageTitle);
        }

        RandomDelayUtil.shortDelay();

        String profileUrl = resolveProfileUrl(page, postUrl);
        if (profileUrl == null) {
            log.debug("[PROFILE] Could not resolve profile link from: {}", postUrl);
            return null;
        }

        page.navigate(profileUrl);
        try {
            page.waitForLoadState(
                LoadState.NETWORKIDLE,
                new Page.WaitForLoadStateOptions().setTimeout(PAGE_TIMEOUT_MS));
        } catch (TimeoutError te) {
            log.debug("[PROFILE] Network idle timeout for profile, continuing anyway: {}", profileUrl);
            // Don't return null — page may still have loaded enough content
        }

        // Give JS extra time to inject profile data into the DOM
        RandomDelayUtil.delay(1500, 2500, "profile-settle");

        // Check for soft-login wall (Instagram sometimes shows this even with session)
        String profilePageTitle = page.title();
        if (profilePageTitle != null &&
                (profilePageTitle.contains("Log in") || profilePageTitle.contains("Sign up"))) {
            log.warn("[PROFILE] Login wall detected on profile page — session may be rate-limited");
            // Don't invalidate session — this is rate limiting, not expiry
            // Just skip this profile
            return null;
        }

        RandomDelayUtil.shortDelay();

        String username = extractUsername(page.url());
        if (username == null) {
            log.debug("[PROFILE] Cannot determine username from URL: {}", page.url());
            return null;
        }

        log.info("[PROFILE-DEBUG] Profile URL: {}", profileUrl);
        log.info("[PROFILE-DEBUG] Profile page title: {}", page.title());
        log.info("[PROFILE-DEBUG] Profile page actual URL: {}", page.url());

        // Log ALL external links on the profile page
        try {
            List<ElementHandle> allLinks = page.querySelectorAll("a[href]");
            log.info("[PROFILE-DEBUG] Total <a> tags on profile: {}", allLinks.size());
            int shown = 0;
            for (ElementHandle a : allLinks) {
                String href = a.getAttribute("href");
                if (href != null && !href.startsWith("/") && !href.contains("instagram.com")) {
                    log.info("[PROFILE-DEBUG]   External link found: {}", href);
                    shown++;
                }
                if (shown >= 10) break;
            }
            if (shown == 0) {
                log.warn("[PROFILE-DEBUG]   NO external links found on profile @{}", username);
                // Log first 5 any links to see what's there
                int count = 0;
                for (ElementHandle a : allLinks) {
                    String href = a.getAttribute("href");
                    log.info("[PROFILE-DEBUG]   Any link[{}]: {}", count, href);
                    count++;
                    if (count >= 5) break;
                }
            }
        } catch (Exception e) {
            log.warn("[PROFILE-DEBUG] Could not query links: {}", e.getMessage());
        }

        // Log a snippet of raw HTML to see what Instagram is actually rendering
        try {
            String html = page.content();
            if (html != null && html.length() > 200) {
                // Look for external_url in the page source
                int idx = html.indexOf("external_url");
                if (idx >= 0) {
                    log.info("[PROFILE-DEBUG] Found 'external_url' in HTML at idx {}: ...{}...",
                        idx, html.substring(idx, Math.min(idx + 100, html.length())));
                } else {
                    log.warn("[PROFILE-DEBUG] 'external_url' NOT found in page HTML");
                }
                // Look for bio_links
                int idx2 = html.indexOf("bio_links");
                if (idx2 >= 0) {
                    log.info("[PROFILE-DEBUG] Found 'bio_links' in HTML: ...{}...",
                        html.substring(idx2, Math.min(idx2 + 150, html.length())));
                } else {
                    log.warn("[PROFILE-DEBUG] 'bio_links' NOT found in page HTML");
                }
            }
        } catch (Exception e) {
            log.warn("[PROFILE-DEBUG] Could not read page HTML: {}", e.getMessage());
        }

        String bio = extractBio(page);
        String websiteUrl = extractWebsiteLink(page);

        if (websiteUrl == null || websiteUrl.isBlank()) {
            log.debug("[PROFILE] No external link for @{} - skipping", username);
            return null;
        }

        return BrandDto.builder()
                .username(username)
                .bio(bio)
                .websiteUrl(websiteUrl)
                .sourcePostUrl(postUrl)
                .build();
    }

    private String resolveProfileUrl(Page page, String postUrl) {

        // METHOD 1: Extract username from current page URL
        // Instagram redirects /p/CODE/ to the post, URL stays as /p/CODE/
        // But if we're on a reel or post, username IS extractable from the HTML
        String usernameFromUrl = extractUsername(page.url());
        if (usernameFromUrl != null) {
            log.debug("[PROFILE] Username from page URL: {}", usernameFromUrl);
            return "https://www.instagram.com/" + usernameFromUrl + "/";
        }

        // METHOD 2: Extract from JSON embedded in page source (most reliable)
        try {
            String html = page.content();
            if (html != null) {
                // Pattern 1: owner username in post JSON
                Matcher m1 = Pattern.compile("\"owner\":\\{[^}]*\"username\":\"([A-Za-z0-9._]+)\"")
                    .matcher(html);
                if (m1.find()) {
                    String u = m1.group(1);
                    if (!RESERVED_PATHS.contains(u.toLowerCase())) {
                        log.debug("[PROFILE] Username from owner JSON: {}", u);
                        return "https://www.instagram.com/" + u + "/";
                    }
                }

                // Pattern 2: user JSON block
                Matcher m2 = Pattern.compile("\"user\":\\{[^}]*\"username\":\"([A-Za-z0-9._]+)\"")
                    .matcher(html);
                if (m2.find()) {
                    String u = m2.group(1);
                    if (!RESERVED_PATHS.contains(u.toLowerCase())) {
                        log.debug("[PROFILE] Username from user JSON: {}", u);
                        return "https://www.instagram.com/" + u + "/";
                    }
                }

                // Pattern 3: any username field
                Matcher m3 = Pattern.compile("\"username\":\"([A-Za-z0-9._]+)\"").matcher(html);
                while (m3.find()) {
                    String u = m3.group(1);
                    if (!RESERVED_PATHS.contains(u.toLowerCase()) && u.length() > 2) {
                        log.debug("[PROFILE] Username from generic JSON: {}", u);
                        return "https://www.instagram.com/" + u + "/";
                    }
                }
            }
        } catch (Exception e) {
            log.trace("[PROFILE] JSON extraction failed: {}", e.getMessage());
        }

        // METHOD 3: Page title parsing
        try {
            String title = page.title();
            if (title != null) {
                // Format: "Username • Instagram photos and videos"
                // Format: "@username"
                // Format: "Username on Instagram: ..."
                String[] patterns = {
                    " • Instagram",
                    " on Instagram",
                    " (@",
                    " | Instagram"
                };
                for (String pat : patterns) {
                    if (title.contains(pat)) {
                        String candidate = title.split(Pattern.quote(pat))[0].trim()
                            .replace("@", "").replaceAll("\\s+", "");
                        if (!candidate.isBlank() && candidate.length() < 40
                                && !RESERVED_PATHS.contains(candidate.toLowerCase())) {
                            log.debug("[PROFILE] Username from title '{}': {}", title, candidate);
                            return "https://www.instagram.com/" + candidate + "/";
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // METHOD 4: DOM selectors for profile link in post header
        List<String> profileSelectors = List.of(
            "header a[href^='/'][href!='/']",
            "article header a[href^='/']",
            "a[href^='/'][role='link']:not([href*='/p/']):not([href*='/reel/'])",
            "span[class] a[href^='/']",
            "div[class] a[href^='/'][href!='/explore/']"
        );

        for (String sel : profileSelectors) {
            try {
                List<ElementHandle> els = page.querySelectorAll(sel);
                for (ElementHandle el : els) {
                    String href = el.getAttribute("href");
                    if (href == null || href.isBlank() || href.equals("/")) continue;
                    String absolute = href.startsWith("http")
                        ? href : "https://www.instagram.com" + href;
                    String username = extractUsername(absolute.split("\\?")[0]);
                    if (username != null) {
                        log.debug("[PROFILE] Username from DOM selector '{}': {}", sel, username);
                        return "https://www.instagram.com/" + username + "/";
                    }
                }
            } catch (Exception ignored) {}
        }

        log.warn("[PROFILE] Could not resolve profile URL from post: {}", postUrl);
        return null;
    }

    private String extractUsername(String url) {
        if (url == null) return null;
        Pattern usernamePattern = Pattern.compile("instagram\\.com/([A-Za-z0-9._]+)(?:/|$)");
        Matcher m = usernamePattern.matcher(url);
        if (m.find()) {
            String u = m.group(1);
            if (RESERVED_PATHS.contains(u.toLowerCase())) return null;
            return u;
        }
        return null;
    }

    private String extractBio(Page page) {
        List<String> bioSelectors = List.of(
                "header section div[dir='auto']",
                "header section span[class]",
                "section span[class]",
                "div[class*='biography'] span",
                "span[class*='bio']"
        );
        for (String sel : bioSelectors) {
            try {
                ElementHandle el = page.querySelector(sel);
                if (el != null) {
                    String text = el.innerText();
                    if (text != null && text.length() > 5) return text.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String extractWebsiteLink(Page page) {

        // METHOD 1: JSON embedded in page source — most reliable, Instagram always includes this
        try {
            String html = page.content();
            if (html != null) {
                // external_url field (classic API)
                Matcher m1 = Pattern.compile("\"external_url\":\"(https?://[^\"]+)\"").matcher(html);
                if (m1.find()) {
                    String url = m1.group(1).replace("\\/", "/");
                    String normalized = normalizeExternalUrl(url);
                    if (isUsableUrl(normalized)) {
                        log.info("[PROFILE] Website via external_url JSON: {}", normalized);
                        return normalized;
                    }
                }

                // bio_links array (newer API format)
                Matcher m2 = Pattern.compile("\"bio_links\":\\[\\{\"url\":\"(https?://[^\"]+)\"")
                    .matcher(html);
                if (m2.find()) {
                    String url = m2.group(1).replace("\\/", "/");
                    String normalized = normalizeExternalUrl(url);
                    if (isUsableUrl(normalized)) {
                        log.info("[PROFILE] Website via bio_links JSON: {}", normalized);
                        return normalized;
                    }
                }

                // Any http URL near "website" or "url" key in JSON
                Matcher m3 = Pattern.compile("\"(?:website|url|link)\":\"(https?://[^\"]{10,})\"")
                    .matcher(html);
                while (m3.find()) {
                    String url = m3.group(1).replace("\\/", "/");
                    String normalized = normalizeExternalUrl(url);
                    if (isUsableUrl(normalized)) {
                        log.info("[PROFILE] Website via generic JSON url field: {}", normalized);
                        return normalized;
                    }
                }
            }
        } catch (Exception e) {
            log.trace("[PROFILE] JSON website extraction failed: {}", e.getMessage());
        }

        // METHOD 2: DOM selectors (updated for 2024-2025 Instagram DOM)
        List<String> updatedSelectors = List.of(
            "a[href*='l.instagram.com']",
            "a[href*='://'][role='link']",
            "div[class*='_aa_5'] a",
            "div[class*='x9f619'] a[href*='://']",
            "span[class*='x193iq5w'] a[href*='://']",
            "a[href*='linktree']",
            "a[href*='beacons']",
            "a[href*='bio.link']",
            "a[href*='linkpop']",
            "a[href*='hoo.be']",
            "a[href^='http'][rel*='nofollow']",
            "a[href^='http']:not([href*='instagram']):not([href*='facebook'])" +
                ":not([href*='twitter']):not([href*='youtube']):not([href*='tiktok'])"
        );

        for (String sel : updatedSelectors) {
            try {
                List<ElementHandle> els = page.querySelectorAll(sel);
                for (ElementHandle el : els) {
                    String href = el.getAttribute("href");
                    String normalized = normalizeExternalUrl(href);
                    if (isUsableUrl(normalized)) {
                        log.info("[PROFILE] Website via DOM selector '{}': {}", sel, normalized);
                        return normalized;
                    }
                }
            } catch (Exception ignored) {}
        }

        // METHOD 3: Visible body text URL scan
        try {
            String bodyText = page.innerText("body");
            Matcher m = TEXT_URL_PATTERN.matcher(bodyText);
            while (m.find()) {
                String normalized = normalizeExternalUrl(m.group());
                if (isUsableUrl(normalized)) {
                    log.info("[PROFILE] Website via body text scan: {}", normalized);
                    return normalized;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String normalizeExternalUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.trim();

        if (trimmed.startsWith("https://l.instagram.com/") || trimmed.startsWith("http://l.instagram.com/")) {
            try {
                URI uri = URI.create(trimmed);
                String query = uri.getRawQuery();
                if (query != null) {
                    String encoded = Arrays.stream(query.split("&"))
                            .filter(p -> p.startsWith("u="))
                            .map(p -> p.substring(2))
                            .findFirst()
                            .orElse(null);
                    if (encoded != null) {
                        String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                        if (decoded.startsWith("http")) {
                            trimmed = decoded;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return null;
            String path = uri.getPath() == null ? "" : uri.getPath();
            return scheme + "://" + host + path;
        } catch (Exception e) {
            return trimmed.split("\\?")[0];
        }
    }

    private boolean isUsableUrl(String url) {
        if (url == null || url.isBlank()) return false;
        if (!url.startsWith("http")) return false;

        String lower = url.toLowerCase();
        if (lower.contains("instagram.com")) return false;

        // Step 2D — Filter the brand
        if (REJECT_URL.matcher(url).find()) return false;

        // Brand name matches (check against hostname or parts of it)
        try {
            String host = URI.create(url).getHost();
            if (host != null) {
                String hostLower = host.toLowerCase();
                for (String rejected : REJECTED_BRAND_NAMES) {
                    if (hostLower.contains(rejected)) return false;
                }
            }
        } catch (Exception ignored) {}

        return true;
    }

    private BrowserContext createContext(Browser browser) {
        return browser.newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(sessionManager.getSessionPath())
                        .setViewportSize(1280, 900)
                        .setUserAgent(USER_AGENT));
    }

    private boolean isLoginPage(String url) {
        return url != null
                && (url.contains("/accounts/login") || url.contains("/accounts/emailsignup"));
    }
}
