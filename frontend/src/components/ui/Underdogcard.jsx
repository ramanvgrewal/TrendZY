export default function UnderdogCard({ product }) {
    const shopUrl = product.shopUrl || product.websiteLink;
    const imageUrl = product.imageUrl || product.primaryImageUrl || product.image;

    const mainPrice = product.mainPrice || product.priceInr || product.price;
    const discountedPrice = product.discountedPrice;
    const displayPrice = discountedPrice || mainPrice;

    const discountPct =
        discountedPrice && mainPrice && mainPrice > discountedPrice
            ? Math.round(((mainPrice - discountedPrice) / mainPrice) * 100)
            : null;

    const sectionColors = {
        CRICKET: '#fbbf24',
        GYM: '#f97316',
        ANIME: '#c084fc',
        STREETWEAR: '#60a5fa',
        SNEAKERS: '#34d399',
        CODING: '#a3e635',
    };

    const sectionEmojis = {
        CRICKET: '🏏',
        GYM: '💪',
        ANIME: '⚔️',
        STREETWEAR: '🛹',
        SNEAKERS: '👟',
        CODING: '💻',
    };

    const accent = sectionColors[product.section] || '#c4b5fd';
    const sectionEmoji = sectionEmojis[product.section] || '✦';

    return (
        <div
            style={{
                background: 'hsl(0 0% 9%)',
                border: '1px solid hsl(0 0% 16%)',
                borderRadius: 12,
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                transition: 'transform 260ms ease, box-shadow 260ms ease, border-color 260ms ease',
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = `0 16px 48px rgba(0,0,0,0.55), 0 0 20px ${accent}1a`;
                e.currentTarget.style.borderColor = `${accent}44`;
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = '';
                e.currentTarget.style.boxShadow = '';
                e.currentTarget.style.borderColor = 'hsl(0 0% 16%)';
            }}
        >
            {/* IMAGE */}
            <div style={{ position: 'relative', width: '100%', background: '#141414', overflow: 'hidden' }}>
                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt={product.productName}
                        loading="lazy"
                        onError={(e) => { e.currentTarget.style.display = 'none'; }}
                        style={{
                            width: '100%',
                            height: 220,
                            objectFit: 'cover',
                            transition: 'transform 480ms ease',
                        }}
                        onMouseEnter={(e) => (e.target.style.transform = 'scale(1.05)')}
                        onMouseLeave={(e) => (e.target.style.transform = 'scale(1)')}
                    />
                ) : (
                    <div style={{
                        width: '100%',
                        height: 220,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: 48,
                        color: 'rgba(255,255,255,0.1)',
                    }}>
                        {sectionEmoji}
                    </div>
                )}

                {/* gradient */}
                <div style={{
                    position: 'absolute',
                    inset: 0,
                    background: 'linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 50%)',
                }} />

                {/* section badge */}
                {product.section && (
                    <div style={{ position: 'absolute', top: 8, left: 8 }}>
                        <span className="font-mono" style={{
                            fontSize: 8,
                            fontWeight: 700,
                            letterSpacing: '0.14em',
                            padding: '3px 8px',
                            borderRadius: 4,
                            background: `${accent}20`,
                            color: accent,
                            border: `1px solid ${accent}35`,
                        }}>
                            {sectionEmoji} {product.section}
                        </span>
                    </div>
                )}

                {/* discount */}
                {discountPct && (
                    <div style={{ position: 'absolute', top: 8, right: 8 }}>
                        <span className="font-mono" style={{
                            fontSize: 9,
                            fontWeight: 700,
                            padding: '3px 8px',
                            borderRadius: 4,
                            background: 'rgba(239,68,68,0.2)',
                            color: '#ef4444',
                        }}>
                            -{discountPct}%
                        </span>
                    </div>
                )}
            </div>

            {/* CONTENT */}
            <div style={{ padding: 12, display: 'flex', flexDirection: 'column', flex: 1 }}>
                <h3>{product.productName}</h3>

                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    marginTop: 10,
                }}>
                    <span>
                        ₹{displayPrice}
                    </span>

                    {/* ✅ FIXED BUTTON */}
                    {shopUrl && (
                        <a
                            href={shopUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            onClick={(e) => e.stopPropagation()}
                            className="font-mono"
                            style={{
                                fontSize: 10,
                                fontWeight: 700,
                                padding: '6px 12px',
                                borderRadius: 7,
                                background: accent,
                                color: '#000',
                                textDecoration: 'none',
                            }}
                        >
                            SHOP →
                        </a>
                    )}
                </div>
            </div>
        </div>
    );
}