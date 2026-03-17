import { Link } from 'react-router-dom';

export default function CuratedCard({ product }) {
    const hasImage = product.imageUrl && product.imageUrl.trim() !== '';

    return (
        <article className="curated-card group">
            {/* ✦ Exclusive badge */}
            <div className="curated-badge">✦ Exclusive</div>

            {/* Product image */}
            <div className="curated-card-image">
                {hasImage ? (
                    <img src={product.imageUrl} alt={product.productName} loading="lazy" />
                ) : (
                    <div className="curated-card-placeholder">
                        <span>✦</span>
                    </div>
                )}
            </div>

            {/* Content */}
            <div className="curated-card-body">
                <p className="curated-brand">{product.brandName}</p>
                <h3 className="curated-product-name">{product.productName}</h3>

                {/* Price */}
                <div className="curated-price-row">
                    {product.priceInr && (
                        <span className="curated-price">₹{product.priceInr.toLocaleString()}</span>
                    )}
                    {product.priceRange && (
                        <span className="curated-price-range">({product.priceRange})</span>
                    )}
                </div>

                {/* Description */}
                {product.description && (
                    <p className="curated-desc">{product.description}</p>
                )}

                {/* Source handle */}
                {product.sourceHandle && (
                    <p className="curated-source">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }}>
                            <rect x="2" y="2" width="20" height="20" rx="5" ry="5" /><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z" /><line x1="17.5" y1="6.5" x2="17.51" y2="6.5" />
                        </svg>
                        {product.sourceHandle}
                    </p>
                )}

                {/* Vibe tags */}
                {product.vibeTags && product.vibeTags.length > 0 && (
                    <div className="curated-tags">
                        {product.vibeTags.slice(0, 3).map(tag => (
                            <span key={tag} className="curated-tag">#{tag}</span>
                        ))}
                    </div>
                )}

                {/* Shop Now button */}
                <a
                    href={product.websiteLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="curated-shop-btn"
                    onClick={(e) => e.stopPropagation()}
                >
                    Shop Now →
                </a>
            </div>
        </article>
    );
}
