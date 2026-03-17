package com.trendzy.service;

import com.trendzy.dto.response.ProductImageResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Extracts real product images from ecommerce product pages only.
 * Flow: search URL → first product link → product page → og:image (or main product image).
 * Does NOT use search pages, logos, or stock image APIs.
 */
@Service
@Slf4j
public class ProductImageExtractorService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9";
    private static final int TIMEOUT_MS = 12_000;

    private static final Pattern LOGO_OR_BANNER = Pattern.compile(
            "logo|banner|brand|placeholder|default\\.(jpg|png|webp)|/images/logo|favicon",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * From a search page URL, get the first product link, then extract image from that product page.
     * Priority: Amazon → Myntra → Flipkart (caller decides order).
     * Returns null if no valid product image could be extracted.
     */
    public ProductImageResult extractProductImageFromSearchUrl(String searchUrl) {
        if (searchUrl == null || searchUrl.isBlank()) {
            return null;
        }
        try {
            String productPageUrl = extractFirstProductLinkFromSearch(searchUrl);
            if (productPageUrl == null || productPageUrl.isBlank()) {
                log.debug("[IMAGE] No product link found on search page: {}", maskUrl(searchUrl));
                return null;
            }
            String imageUrl = extractImageFromProductPage(productPageUrl);
            if (imageUrl == null || imageUrl.isBlank() || isLikelyLogoOrBanner(imageUrl)) {
                log.debug("[IMAGE] No valid product image on product page: {}", maskUrl(productPageUrl));
                return null;
            }
            return ProductImageResult.builder()
                    .imageUrl(imageUrl)
                    .productPageUrl(productPageUrl)
                    .build();
        } catch (Exception e) {
            log.warn("[IMAGE] Failed for search URL {} : {}", maskUrl(searchUrl), e.getMessage());
            return null;
        }
    }

    /**
     * Fetch search page and return the first real product page URL (absolute).
     */
    public String extractFirstProductLinkFromSearch(String searchUrl) {
        if (searchUrl == null || searchUrl.isBlank()) return null;
        Document doc = fetchDocument(searchUrl);
        if (doc == null) return null;

        String baseHost = getBaseHost(searchUrl);
        if (baseHost == null) return null;

        if (searchUrl.contains("amazon.")) {
            return extractFirstAmazonProductLink(doc, searchUrl);
        }
        if (searchUrl.contains("myntra.com")) {
            return extractFirstMyntraProductLink(doc, searchUrl);
        }
        if (searchUrl.contains("flipkart.com")) {
            return extractFirstFlipkartProductLink(doc, searchUrl);
        }
        return null;
    }

    /**
     * Fetch product page and return og:image; if missing, try site-specific main image selector.
     */
    public String extractImageFromProductPage(String productPageUrl) {
        if (productPageUrl == null || productPageUrl.isBlank()) return null;
        Document doc = fetchDocument(productPageUrl);
        if (doc == null) return null;

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (content != null && !content.isBlank() && !isLikelyLogoOrBanner(content)) {
                return content.trim();
            }
        }

        if (productPageUrl.contains("amazon.")) {
            return extractAmazonMainImage(doc);
        }
        if (productPageUrl.contains("myntra.com")) {
            return extractMyntraMainImage(doc);
        }
        if (productPageUrl.contains("flipkart.com")) {
            return extractFlipkartMainImage(doc);
        }
        return null;
    }

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", ACCEPT_LANGUAGE)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
        } catch (Exception e) {
            log.debug("[IMAGE] Fetch failed for {} : {}", maskUrl(url), e.getMessage());
            return null;
        }
    }

    private String extractFirstAmazonProductLink(Document doc, String searchUrl) {
        Elements productLinks = doc.select("div[data-component-type=s-search-result] h2 a.a-link-normal[href*=/dp/]");
        if (productLinks.isEmpty()) {
            productLinks = doc.select("a[href*=/dp/]");
        }
        for (Element a : productLinks) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) href = a.attr("href");
            if (href != null && !href.isBlank() && href.contains("/dp/")) {
                String clean = href.split("\\?")[0].trim();
                return toAbsolute(clean, searchUrl);
            }
        }
        return null;
    }

    private String extractFirstMyntraProductLink(Document doc, String searchUrl) {
        Elements links = doc.select("a[href*=/shop/], a[href*=/buy/], li.product-base a[href^=/]");
        if (links.isEmpty()) {
            links = doc.select("a[href^=/][href*=-]");
        }
        for (Element a : links) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) href = a.attr("href");
            if (href == null || href.isBlank()) continue;
            if (href.startsWith("/")) href = "https://www.myntra.com" + href.split("\\?")[0];
            else href = href.split("\\?")[0];
            if (href.contains("myntra.com") && !href.contains("/search") && !href.contains("/cart")) {
                return href;
            }
        }
        return null;
    }

    private String extractFirstFlipkartProductLink(Document doc, String searchUrl) {
        Elements links = doc.select("a[href*=/p/]");
        for (Element a : links) {
            String href = a.attr("abs:href");
            if (href == null || href.isBlank()) href = a.attr("href");
            if (href != null && !href.isBlank() && href.contains("/p/")) {
                String clean = href.split("\\?")[0].trim();
                if (clean.contains("flipkart.com") || clean.startsWith("/")) {
                    return toAbsolute(clean, searchUrl);
                }
            }
        }
        return null;
    }

    private String extractAmazonMainImage(Document doc) {
        Element img = doc.selectFirst("#landingImage");
        if (img == null) img = doc.selectFirst("img#imgBlkFront");
        if (img == null) img = doc.selectFirst("#main-image, .a-dynamic-image[data-a-dynamic-image]");
        if (img != null) {
            String src = img.attr("src");
            if (src != null && !src.isBlank() && !isLikelyLogoOrBanner(src)) return src;
            src = img.attr("data-old-hires");
            if (src != null && !src.isBlank() && !isLikelyLogoOrBanner(src)) return src;
        }
        return null;
    }

    private String extractMyntraMainImage(Document doc) {
        Element img = doc.selectFirst(".image-grid-image, .pdp-images img, img[class*=product]");
        if (img == null) img = doc.selectFirst("img[src*='myntra']");
        if (img != null) {
            String src = img.attr("src");
            if (src != null && !src.isBlank() && !isLikelyLogoOrBanner(src)) return src;
        }
        return null;
    }

    private String extractFlipkartMainImage(Document doc) {
        Element img = doc.selectFirst("._396QI4, img[class*=_396QI4], ._2r_T1I");
        if (img == null) img = doc.selectFirst(".CXW8mj img");
        if (img != null) {
            String src = img.attr("src");
            if (src != null && !src.isBlank() && !isLikelyLogoOrBanner(src)) return src;
        }
        return null;
    }

    private boolean isLikelyLogoOrBanner(String url) {
        if (url == null || url.isBlank()) return true;
        return LOGO_OR_BANNER.matcher(url).find();
    }

    private String getBaseHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String toAbsolute(String href, String baseUrl) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("http")) return href.split("\\?")[0].trim();
        String base = getBaseHost(baseUrl);
        if (base == null) return href;
        if (href.startsWith("/")) return base + href.split("\\?")[0].trim();
        return base + "/" + href.split("\\?")[0].trim();
    }

    private static String maskUrl(String url) {
        if (url == null || url.length() < 50) return url;
        return url.substring(0, 50) + "...";
    }
}
