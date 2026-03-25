export const filterAndSortProducts = (products) => {
  if (!products || !Array.isArray(products)) return [];
  
  // Temporarily log raw API products for debugging
  console.log("Products from API:", products);

  // STRICT DATA FILTERING: Only show completely valid, enriched products
  const validProducts = products.filter(p => {
    const hasImage = p.image || p.imageUrl || p.primaryImageUrl;
    const hasShopUrl = p.shopUrl;
    
    // Curated products don't have enrichmentStatus. Trends use COMPLETED or SUCCESS.
    const hasValidStatus = p.enrichmentStatus === undefined || 
                           p.enrichmentStatus === null || 
                           p.enrichmentStatus === "SUCCESS" || 
                           p.enrichmentStatus === "COMPLETED";

    return hasShopUrl && hasImage && hasValidStatus;
  });
  
  // DEDUPLICATION: Ensure no duplicate products are shown
  const uniqueMap = new Map();
  validProducts.forEach(p => {
    // Generate a unique key for the product (prioritizing shopUrl, fallback to name+brand)
    const key = p.shopUrl || `${(p.productName || '').toLowerCase()}::${(p.brandName || '').toLowerCase()}`;
    if (!uniqueMap.has(key)) {
      uniqueMap.set(key, p);
    } else {
      // If duplicate found, keep the one with higher matchScore/trendScore
      const existing = uniqueMap.get(key);
      const currentScore = Math.max(p.matchScore || 0, p.trendScore || 0);
      const existingScore = Math.max(existing.matchScore || 0, existing.trendScore || 0);
      if (currentScore > existingScore) {
        uniqueMap.set(key, p);
      }
    }
  });

  const uniqueProducts = Array.from(uniqueMap.values());

  // Sort by matchScore descending
  return uniqueProducts.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));
};
