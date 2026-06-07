package com.trendzy.ingestion.scraper.pinterest;

import com.microsoft.playwright.*;
import com.trendzy.ingestion.scraper.util.RandomDelayUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the Pinterest Playwright session file stored at
 * {@code ~/{@value #SESSION_FILE_NAME}}.
 *
 * <ul>
 *   <li>Session present → callers use it for headless scraping.</li>
 *   <li>Session absent → {@link #ensureSession(Playwright)} opens a headful browser
 *       and waits up to {@value #LOGIN_TIMEOUT_MS} ms for the operator to log in.</li>
 *   <li>Session expired → detected by redirect to login page; file is deleted
 *       and must be re-created on the next call.</li>
 * </ul>
 */
@Component
@Slf4j
public class PinterestSessionManager {

    private static final String SESSION_FILE_NAME  = "pinterest_session.json";
    private static final long   LOGIN_TIMEOUT_MS   = 120_000L;
    private static final String PINTEREST_LOGIN    = "https://www.pinterest.com/login/";
    private static final String USER_AGENT         =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";

    @Value("${scraper.pinterest.session-dir:${user.home}/.trendzy}")
    private String sessionDir;

    private Path sessionPath;

    // ─────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        sessionPath = Paths.get(sessionDir, SESSION_FILE_NAME);
        try {
            Files.createDirectories(sessionPath.getParent());
            log.info("[PINTEREST-SESSION] Session directory: {}", sessionPath.getParent());
        } catch (IOException e) {
            log.error("[PINTEREST-SESSION] Cannot create session directory: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /** {@code true} when a non-trivial session file is present and readable. */
    public boolean sessionExists() {
        return sessionPath != null
                && Files.exists(sessionPath)
                && Files.isReadable(sessionPath)
                && sessionPath.toFile().length() > 20;
    }

    /** Path to the JSON session file (may not exist yet). */
    public Path getSessionPath() {
        return sessionPath;
    }

    /**
     * Ensures a valid session is present.
     * If the session file is absent this method blocks for up to 2 minutes
     * while the operator logs in through the headful browser window.
     *
     * @return {@code true} if a session is available after this call
     */
    public boolean ensureSession(Playwright playwright) {
        if (sessionExists()) {
            log.info("[PINTEREST-SESSION] Existing session found — skipping login");
            return true;
        }

        log.info("[PINTEREST-SESSION] No session found — opening headful browser for manual login");
        return performManualLogin(playwright);
    }

    /**
     * Saves the current browser context to disk.
     *
     * @param context a live, authenticated {@link BrowserContext}
     */
    public void saveSession(BrowserContext context) {
        try {
            context.storageState(
                    new BrowserContext.StorageStateOptions().setPath(sessionPath));
            log.info("[PINTEREST-SESSION] ✅ Session saved → {}", sessionPath);
        } catch (Exception e) {
            log.error("[PINTEREST-SESSION] Failed to save session: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes the session file so that the next pipeline run triggers fresh login.
     * Call this when a redirect to the login page is detected.
     */
    public void invalidateSession() {
        try {
            boolean deleted = Files.deleteIfExists(sessionPath);
            log.info("[PINTEREST-SESSION] Session invalidated — file deleted={}", deleted);
        } catch (IOException e) {
            log.error("[PINTEREST-SESSION] Failed to delete session file: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MANUAL LOGIN
    // ─────────────────────────────────────────────────────────────

    private boolean performManualLogin(Playwright playwright) {
        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(80);

        try (Browser browser = playwright.chromium().launch(opts)) {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1280, 900)
                            .setUserAgent(USER_AGENT));

            Page page = context.newPage();
            page.navigate(PINTEREST_LOGIN);

            log.info("[PINTEREST-SESSION] Waiting {} seconds for manual login...", LOGIN_TIMEOUT_MS / 1000);

            try {
                // Wait until the user is no longer on the login/signup routes
                page.waitForURL(
                        url -> !url.contains("/login") && !url.contains("/signup"),
                        new Page.WaitForURLOptions().setTimeout(LOGIN_TIMEOUT_MS));

                log.info("[PINTEREST-SESSION] Login detected at: {}", page.url());
                // Short delay to let auth cookies settle
                RandomDelayUtil.delay();
                saveSession(context);
                context.close();
                return true;

            } catch (TimeoutError te) {
                log.warn("[PINTEREST-SESSION] Login timed out after {}s", LOGIN_TIMEOUT_MS / 1000);
                context.close();
                return false;
            }
        } catch (Exception e) {
            log.error("[PINTEREST-SESSION] Manual login flow failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
