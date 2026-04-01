package com.trendzy.scraper.validator;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.trendzy.scraper.util.RandomDelayUtil;
import com.trendzy.scraper.website.RawProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates scraped products by checking purchasability signals on each product page.
 */
@Component
@Slf4j
public class ProductValidator {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36";
    private static final int PAGE_TIMEOUT_MS = 15_000;

    private static final List<String> CART_BUTTON_SELECTORS = List.of(
            "button[type='submit'][name='add']",
            "button[class*='add-to-cart']",
            "button[class*='AddToCart']",
            "button[id*='add-to-cart']",
            "button[data-testid*='add-to-cart']",
            "input[type='submit'][value*='Add']",
            "input[type='submit'][value*='Cart']",
            "button:has-text('Add to Cart')",
            "button:has-text('Add to Bag')",
            "button:has-text('Buy Now')",
            "button:has-text('Shop Now')"
    );

    private static final List<String> PRICE_SELECTORS = List.of(
            "[data-product-price]",
            "[itemprop='price']",
            "meta[property='product:price:amount']",
            "meta[name='twitter:data1']",
            ".price",
            ".money",
            "[class*='price']"
    );

    private static final List<String> IMAGE_SELECTORS = List.of(
            "meta[property='og:image']",
            "meta[name='twitter:image']",
            "img[src*='cdn.shopify.com']",
            "img[src*='cloudinary.com']",
            "img[srcset*='cdn.shopify.com']",
            "img[srcset*='cloudinary.com']",
            "[class*='product'] img",
            "main img"
    );

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([0-9][0-9,]*)", Pattern.CASE_INSENSITIVE);

    public List<RawProduct> validateProducts(List<RawProduct> candidates, Playwright playwright) {
        List<RawProduct> valid = new ArrayList<>();

        if (candidates == null || candidates.isEmpty()) return valid;

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);

        try (Browser browser = playwright.chromium().launch(launchOpts)) {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1280, 900)
                            .setUserAgent(USER_AGENT));
            Page page = context.newPage();

            for (RawProduct product : candidates) {
                try {
                    boolean passed = validateProduct(page, product);
                    if (passed) {
                        product.setValidated(true);
                        valid.add(product);
                        log.info("[VALIDATOR] Passed: '{}'", product.getProductName());
                    } else {
                        log.debug("[VALIDATOR] Failed: '{}'", product.getProductName());
                    }
                    RandomDelayUtil.shortDelay();
                } catch (Exception e) {
                    log.warn("[VALIDATOR] Exception validating '{}': {}", product.getProductName(), e.getMessage());
                }
            }

            context.close();
        } catch (Exception e) {
            log.error("[VALIDATOR] Browser error during validation: {}", e.getMessage(), e);
        }

        log.info("[VALIDATOR] {}/{} products passed validation", valid.size(), candidates.size());
        return valid;
    }

    private boolean validateProduct(Page page, RawProduct product) {
        if (product.getProductUrl() == null || product.getProductUrl().isBlank()) {
            return false;
        }

        String expectedHost = hostOf(product.getProductUrl());
        Response response;
        try {
            response = page.navigate(product.getProductUrl(), new Page.NavigateOptions().setTimeout(PAGE_TIMEOUT_MS));
            // Wait for DOM to fully load
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(PAGE_TIMEOUT_MS));
        } catch (TimeoutError te) {
            log.warn("[VALIDATOR] TIMEOUT loading: {}", product.getProductUrl());
            return false;
        } catch (Exception e) {
            return false;
        }

        // CHECK 1 — Page loads
        if (response == null || response.status() >= 400) {
            return false;
        }

        String finalUrl = page.url();
        if (isHomeRedirect(product.getProductUrl(), finalUrl, expectedHost)) {
            return false;
        }

        String title = safeLower(page.title());
        if (title.contains("404") || title.contains("not found") || title.contains("page not found") || title.contains("access denied")) {
            return false;
        }
        if (title.contains("just a moment") || title.contains("ddos-guard") || title.contains("captcha")) {
            return false;
        }

        // CHECK 2 — Price visible on page
        Double extractedPrice = extractPrice(page);
        if (extractedPrice == null || extractedPrice <= 0) {
            return false;
        }
        product.setPrice(extractedPrice);
        if (extractedPrice < 400 || extractedPrice > 2000) {
            return false;
        }

        // CHECK 3 — Add to Cart button exists and is enabled
        if (!hasAddToCart(page)) {
            return false;
        }

        // CHECK 4 — Product image
        String verifiedImage = extractAndVerifyImage(page, product.getImageUrl());
        if (verifiedImage == null) {
            // NOTE: If image verification fails but checks 1–3 passed, do NOT reject the product.
            // Instead, keep the product but set imageUrl to the product page URL itself
            product.setImageUrl(product.getProductUrl());
        } else {
            product.setImageUrl(verifiedImage);
        }

        return true;
    }

    private boolean hasAddToCart(Page page) {
        for (String sel : CART_BUTTON_SELECTORS) {
            try {
                ElementHandle el = page.querySelector(sel);
                if (el == null || !el.isVisible()) continue;
                String disabled = el.getAttribute("disabled");
                String ariaDisabled = el.getAttribute("aria-disabled");
                if (disabled == null && (ariaDisabled == null || !"true".equalsIgnoreCase(ariaDisabled))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            List<ElementHandle> buttons = page.querySelectorAll("button, input[type='submit'], a");
            for (ElementHandle b : buttons) {
                if (!b.isVisible()) continue;
                String txt = safeLower(b.innerText());
                if (txt.contains("add to cart") || txt.contains("add to bag") || txt.contains("buy now")) {
                    String disabled = b.getAttribute("disabled");
                    String ariaDisabled = b.getAttribute("aria-disabled");
                    if (disabled == null && (ariaDisabled == null || !"true".equalsIgnoreCase(ariaDisabled))) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private Double extractPrice(Page page) {
        // Step 4 — CHECK 2
        for (String sel : PRICE_SELECTORS) {
            try {
                ElementHandle el = page.querySelector(sel);
                if (el == null) continue;
                String text = el.innerText();
                if ((text == null || text.isBlank()) && (sel.startsWith("meta") || sel.contains("content"))) {
                    text = el.getAttribute("content");
                }
                Double parsed = parsePrice(text);
                if (parsed != null && parsed > 0) {
                    // HANDLE PAISE FORMAT: If extracted price > 10000, divide by 100
                    if (parsed > 10000) parsed /= 100;
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            // Regex patterns as per requirement
            List<Pattern> patterns = List.of(
                Pattern.compile("₹\\s*([0-9][0-9,]*)"),
                Pattern.compile("Rs\\.?\\s*([0-9][0-9,]*)"),
                Pattern.compile("INR\\s*([0-9][0-9,]*)")
            );
            String body = page.innerText("body");
            for (Pattern p : patterns) {
                Matcher m = p.matcher(body);
                if (m.find()) {
                    try {
                        Double parsed = Double.parseDouble(m.group(1).replace(",", ""));
                        if (parsed > 10000) parsed /= 100;
                        return parsed;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Double parsePrice(String text) {
        if (text == null || text.isBlank()) return null;

        Matcher m = PRICE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", ""));
            } catch (Exception ignored) {
            }
        }

        String numeric = text.replaceAll("[^0-9.]", "");
        if (!numeric.isBlank()) {
            try {
                return Double.parseDouble(numeric);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractAndVerifyImage(Page page, String existingImage) {
        List<String> candidates = new ArrayList<>();

        if (existingImage != null && !existingImage.isBlank()) {
            candidates.add(existingImage);
        }

        for (String sel : IMAGE_SELECTORS) {
            try {
                ElementHandle el = page.querySelector(sel);
                if (el == null) continue;

                String url = null;
                if (sel.startsWith("meta")) {
                    url = el.getAttribute("content");
                }
                if (url == null || url.isBlank()) {
                    url = el.getAttribute("src");
                }
                if ((url == null || url.isBlank()) && el.getAttribute("srcset") != null) {
                    String srcset = el.getAttribute("srcset");
                    url = srcset.split(",")[0].trim().split(" ")[0];
                }
                if (url != null && !url.isBlank()) {
                    candidates.add(url);
                }
            } catch (Exception ignored) {
            }
        }

        for (String candidate : candidates) {
            String absolute = toAbsolute(page.url(), candidate);
            if (absolute == null) continue;
            if (isLikelyPlaceholder(absolute)) continue;

            try {
                APIResponse imageResponse = page.request().get(absolute,
                        RequestOptions.create().setTimeout(PAGE_TIMEOUT_MS));
                if (imageResponse != null && imageResponse.status() >= 200 && imageResponse.status() < 300) {
                    String ct = imageResponse.headers().get("content-type");
                    if (ct != null && ct.toLowerCase().startsWith("image/")) {
                        return absolute.split("\\?")[0];
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String toAbsolute(String pageUrl, String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("data:")) return null;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        if (trimmed.startsWith("//")) return "https:" + trimmed;

        try {
            URI base = URI.create(pageUrl);
            return base.resolve(trimmed).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isLikelyPlaceholder(String url) {
        String lower = url.toLowerCase();
        return lower.contains("placeholder")
                || lower.contains("no-image")
                || lower.contains("default")
                || lower.contains("banner")
                || lower.contains("hero");
    }

    private boolean isHomeRedirect(String originalUrl, String finalUrl, String expectedHost) {
        try {
            URI original = URI.create(originalUrl);
            URI fin = URI.create(finalUrl);
            if (expectedHost != null && fin.getHost() != null && !fin.getHost().equalsIgnoreCase(expectedHost)) {
                return true;
            }

            String originalPath = original.getPath() == null ? "" : original.getPath();
            String finalPath = fin.getPath() == null ? "" : fin.getPath();
            boolean originalWasProduct = originalPath.toLowerCase().contains("/product")
                    || originalPath.toLowerCase().contains("/products/")
                    || originalPath.toLowerCase().contains("/shop/");
            boolean finalIsHome = finalPath.isBlank() || "/".equals(finalPath);
            return originalWasProduct && finalIsHome;
        } catch (Exception e) {
            return false;
        }
    }

    private String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeLower(String input) {
        return input == null ? "" : input.toLowerCase();
    }
}
