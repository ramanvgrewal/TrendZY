export const filterAndSortProducts = (products) => {
  if (!products || !Array.isArray(products)) return [];

  const validProducts = products.filter((p) => {
    const hasImage = p.image || p.imageUrl || p.primaryImageUrl;
    // Support both shopUrl (trending enriched) and websiteLink (curated/underdogs)
    const hasShopUrl = p.shopUrl || p.websiteLink;

    // Enrichment status: enriched products must be SUCCESS/COMPLETED
    // Instagram/underdog products won't have enrichmentStatus — let them through
    const hasValidStatus =
        p.enrichmentStatus === undefined ||
        p.enrichmentStatus === null ||
        p.enrichmentStatus === 'SUCCESS' ||
        p.enrichmentStatus === 'COMPLETED';

    return hasShopUrl && hasImage && hasValidStatus;
  });

  // Deduplication by shopUrl or websiteLink
  const uniqueMap = new Map();
  validProducts.forEach((p) => {
    const key =
        p.shopUrl ||
        p.websiteLink ||
        `${(p.productName || '').toLowerCase()}::${(p.brandName || '').toLowerCase()}`;

    if (!uniqueMap.has(key)) {
      uniqueMap.set(key, p);
    } else {
      const existing = uniqueMap.get(key);
      const currentScore = Math.max(p.matchScore || 0, p.trendScore || 0);
      const existingScore = Math.max(existing.matchScore || 0, existing.trendScore || 0);
      if (currentScore > existingScore) uniqueMap.set(key, p);
    }
  });

  return Array.from(uniqueMap.values()).sort(
      (a, b) => (b.matchScore || 0) - (a.matchScore || 0)
  );
};