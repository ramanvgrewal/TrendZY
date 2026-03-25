package com.trendzy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Extracts product images from e-commerce product pages.
 * 
 * Priority order:
 *   1. JSON-LD ({@code <script type="application/ld+json">}) → "image" field
 *   2. og:image meta tag (fallback)
 *   3. Platform-specific img selectors (last resort)
 *
 * Rejects logos, banners, placeholders, and low-resolution images.
 */
@Service
@Slf4j
public class ImageExtractionService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final Pattern REJECT_PATTERN = Pattern.compile(
            "logo|banner|brand[_\\-/]|placeholder|default\\.(jpg|png|webp)|/images/logo|favicon|icon|sprite|1x1|pixel",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOW_RES_PATTERN = Pattern.compile(
            "\\b(\\d{1,2}x\\d{1,2}|50x|x50|32x|x32|16x|x16)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extract the best product image from a product page Document.
     * Returns null if no valid image found.
     */
    public String extractImage(Document doc, String productPageUrl) {
        if (doc == null) return null;

        // ── Priority 1: JSON-LD ──
        String jsonLdImage = extractFromJsonLd(doc);
        if (isValidImage(jsonLdImage)) {
            log.debug("[IMAGE] ✅ JSON-LD image found: {}", truncate(jsonLdImage));
            return jsonLdImage;
        }

        // ── Priority 2: og:image ──
        String ogImage = extractOgImage(doc);
        if (isValidImage(ogImage)) {
            log.debug("[IMAGE] ✅ og:image found: {}", truncate(ogImage));
            return ogImage;
        }

        // ── Priority 3: Platform-specific selectors ──
        String platformImage = extractPlatformSpecificImage(doc, productPageUrl);
        if (isValidImage(platformImage)) {
            log.debug("[IMAGE] ✅ Platform-specific image found: {}", truncate(platformImage));
            return platformImage;
        }

        log.debug("[IMAGE] ❌ No valid image found for {}", truncate(productPageUrl));
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // JSON-LD EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private String extractFromJsonLd(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            try {
                String json = script.html().trim();
                if (json.isEmpty()) continue;

                JsonNode node = JSON_MAPPER.readTree(json);
                String image = findImageInJsonLd(node);
                if (image != null && !image.isBlank()) {
                    return image.trim();
                }
            } catch (Exception e) {
                log.trace("[IMAGE] JSON-LD parse failed: {}", e.getMessage());
            }
        }
        return null;
    }

    private String findImageInJsonLd(JsonNode node) {
        if (node == null) return null;

        // Handle @graph arrays (common in structured data)
        if (node.has("@graph") && node.get("@graph").isArray()) {
            for (JsonNode item : node.get("@graph")) {
                String img = findImageInJsonLd(item);
                if (img != null) return img;
            }
        }

        // Direct "image" field
        JsonNode imageNode = node.path("image");
        if (imageNode.isTextual()) {
            return imageNode.asText();
        }
        if (imageNode.isArray() && imageNode.size() > 0) {
            JsonNode first = imageNode.get(0);
            if (first.isTextual()) return first.asText();
            if (first.has("url")) return first.path("url").asText(null);
            if (first.has("contentUrl")) return first.path("contentUrl").asText(null);
        }
        if (imageNode.isObject()) {
            if (imageNode.has("url")) return imageNode.path("url").asText(null);
            if (imageNode.has("contentUrl")) return imageNode.path("contentUrl").asText(null);
        }

        // Check @type=Product specifically
        String type = node.path("@type").asText("");
        if (type.equalsIgnoreCase("Product")) {
            if (node.has("image")) {
                return extractImageUrl(node.get("image"));
            }
        }

        return null;
    }

    private String extractImageUrl(JsonNode imageNode) {
        if (imageNode == null) return null;
        if (imageNode.isTextual()) return imageNode.asText();
        if (imageNode.isArray() && imageNode.size() > 0) {
            return extractImageUrl(imageNode.get(0));
        }
        if (imageNode.has("url")) return imageNode.path("url").asText(null);
        if (imageNode.has("contentUrl")) return imageNode.path("contentUrl").asText(null);
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // OG:IMAGE EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private String extractOgImage(Document doc) {
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // PLATFORM-SPECIFIC IMG TAG EXTRACTION
    // ─────────────────────────────────────────────────────────────

    private String extractPlatformSpecificImage(Document doc, String url) {
        if (url == null) return null;

        if (url.contains("amazon.")) return extractAmazonImage(doc);
        if (url.contains("myntra.com")) return extractMyntraImage(doc);
        if (url.contains("flipkart.com")) return extractFlipkartImage(doc);
        if (url.contains("meesho.com")) return extractMeeshoImage(doc);

        return null;
    }

    private String extractAmazonImage(Document doc) {
        // Try high-res first
        Element img = doc.selectFirst("#landingImage");
        if (img != null) {
            String hiRes = img.attr("data-old-hires");
            if (isValidImage(hiRes)) return hiRes;
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        img = doc.selectFirst("img#imgBlkFront");
        if (img != null) {
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        img = doc.selectFirst("#main-image, .a-dynamic-image[data-a-dynamic-image]");
        if (img != null) {
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        return null;
    }

    private String extractMyntraImage(Document doc) {
        Element img = doc.selectFirst(".image-grid-image, .pdp-images img, img[class*=product]");
        if (img == null) img = doc.selectFirst("img[src*='myntra']");
        if (img != null) {
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        return null;
    }

    private String extractFlipkartImage(Document doc) {
        Element img = doc.selectFirst("._396QI4, img[class*=_396QI4], ._2r_T1I");
        if (img == null) img = doc.selectFirst(".CXW8mj img");
        if (img != null) {
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        return null;
    }

    private String extractMeeshoImage(Document doc) {
        Element img = doc.selectFirst("img[class*=ProductImage], img[alt*=product]");
        if (img != null) {
            String src = img.attr("src");
            if (isValidImage(src)) return src;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────

    private boolean isValidImage(String url) {
        if (url == null || url.isBlank()) return false;
        if (REJECT_PATTERN.matcher(url).find()) return false;
        if (LOW_RES_PATTERN.matcher(url).find()) return false;
        return true;
    }

    private static String truncate(String s) {
        if (s == null || s.length() < 80) return s;
        return s.substring(0, 80) + "...";
    }
}
