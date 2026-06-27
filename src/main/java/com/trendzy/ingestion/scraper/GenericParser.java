package com.trendzy.ingestion.scraper;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GenericParser {

    public List<RawProduct> extractProducts(Page page, String baseUrl) {
        List<RawProduct> products = new ArrayList<>();
        try {
            page.waitForLoadState();

            // Generic product card selectors
            var productElements = page.querySelectorAll(".product, .item, article, [class*='product']");

            for (ElementHandle element : productElements) {
                try {
                    String title = "";
                    ElementHandle titleEl = element.querySelector("h2, h3, h4, .title");
                    if (titleEl != null) title = titleEl.innerText().trim();
                    if (title.isBlank()) continue;

                    // 👉 SELLING PRICE EXTRACTION
                    Double price = null;
                    ElementHandle priceEl = element.querySelector(".price, .amount, span[class*='price']");
                    if (priceEl != null) {
                        String priceStr = priceEl.innerText().replaceAll("[^0-9.]", "");
                        if (!priceStr.isBlank()) price = Double.parseDouble(priceStr);
                    }

                    // 👉 ORIGINAL PRICE (MRP) EXTRACTION
                    Double originalPrice = null;
                    ElementHandle oldPriceEl = element.querySelector("del, s, strike, .old-price, .original-price, [class*='compare']");
                    if (oldPriceEl != null) {
                        String oldPriceStr = oldPriceEl.innerText().replaceAll("[^0-9.]", "");
                        if (!oldPriceStr.isBlank()) originalPrice = Double.parseDouble(oldPriceStr);
                    }
                    if (originalPrice == null) originalPrice = price; // Fallback to selling price

                    String url = baseUrl;
                    ElementHandle linkEl = element.querySelector("a");
                    if (linkEl != null) {
                        String href = linkEl.getAttribute("href");
                        if (href != null && href.startsWith("http")) url = href;
                        else if (href != null) url = baseUrl + href;
                    }

                    // 👉 IMAGE EXTRACTION
                    String imageUrl = null;
                    ElementHandle imgEl = element.querySelector("img");
                    if (imgEl != null) {
                        imageUrl = imgEl.getAttribute("src");
                        // Fallback for lazy-loaded images
                        if (imageUrl == null || imageUrl.contains("data:image")) {
                            imageUrl = imgEl.getAttribute("data-src");
                        }
                        if (imageUrl != null && imageUrl.startsWith("//")) {
                            imageUrl = "https:" + imageUrl;
                        }
                    }

                    products.add(RawProduct.builder()
                            .productName(title)
                            .mainPrice(price)
                            .originalPrice(originalPrice) // Added to builder
                            .productUrl(url)
                            .imageUrl(imageUrl)
                            .build());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Error in generic parsing: {}", e.getMessage());
        }
        return products;
    }
}