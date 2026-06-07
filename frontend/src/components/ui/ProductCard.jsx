import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { trackClick } from '../../api/affiliate';
import { saveProduct, unsaveProduct } from '../../api/user';
import useAuthStore from '../../store/authStore';
import ScoreRing from './ScoreRing';
import VelocityBadge from './VelocityBadge';

export default function ProductCard({ product, source = 'home_feed', accentColor = '#a3e635' }) {
    const navigate = useNavigate();
    const { isLoggedIn, openAuthModal } = useAuthStore();
    const [isSaved, setIsSaved] = useState(false);

    const productImg = product.image || product.imageUrl || product.primaryImageUrl;

    const validStatus =
        product.enrichmentStatus === undefined ||
        product.enrichmentStatus === null ||
        product.enrichmentStatus === 'SUCCESS' ||
        product.enrichmentStatus === 'COMPLETED';

    const shopUrl = product.shopUrl || product.websiteLink;

    if (!product || !shopUrl || !productImg || !validStatus) return null;

    const isCurated = product.platform === 'brand' || product.tier === 'curated';
    const accent = isCurated ? '#c4b5fd' : accentColor;

    const mainPrice = product.originalPrice || product.price || product.estimatedPrice || product.priceInr;
    // The 'price' from backend is the current selling price
    const discountedPrice = product.originalPrice && product.price < product.originalPrice ? product.price : null;
    const displayPrice = product.price || mainPrice;

    const discountPct = product.discount || (
        discountedPrice && mainPrice && mainPrice > discountedPrice
            ? Math.round(((mainPrice - discountedPrice) / mainPrice) * 100)
            : null
    );

    const handleCardClick = (e) => {
        if (e.target.closest('button') || e.target.closest('a')) return;
        const tier = isCurated ? 'curated' : product.tier || 'trending';
        navigate(`/product/${product.id}?tier=${tier}`);
    };

    const handleVibeClick = (e, tag) => {
        e.stopPropagation();
        navigate(`/vibe/${tag.replace('#', '')}`);
    };

    const handleSave = async (e) => {
        e.stopPropagation();
        if (!isLoggedIn) {
            openAuthModal();
            return;
        }

        const prev = isSaved;
        setIsSaved(!prev);

        try {
            if (prev) {
                await unsaveProduct(product.id);
                toast.success('Removed');
            } else {
                await saveProduct(product.id);
                toast.success('Saved ♥');
            }
        } catch {
            setIsSaved(prev);
            toast.error('Could not update.');
        }
    };

    return (
        <div
            onClick={handleCardClick}
            className="group"
            style={{
                width: '100%',
                cursor: 'pointer',
                background: 'hsl(0 0% 9%)',
                border: '1px solid hsl(0 0% 16%)',
                borderRadius: 12,
                overflow: 'hidden',
                transition: 'transform 260ms ease, box-shadow 260ms ease, border-color 260ms ease',
                display: 'flex',
                flexDirection: 'column',
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
            <div style={{ position: 'relative', width: '100%', background: '#141414', overflow: 'hidden', flexShrink: 0 }}>
                <img
                    src={productImg}
                    alt={product.productName}
                    draggable={false}
                    onError={(e) => { e.target.src = '/fallback.png'; }}
                    className="w-full h-64 object-contain rounded-xl group-hover:scale-105"
                    style={{ transition: 'transform 480ms ease', padding: '12px' }}
                />

                <div
                    style={{
                        position: 'absolute',
                        inset: 0,
                        background: 'linear-gradient(to top, rgba(0,0,0,0.65) 0%, transparent 45%)',
                        pointerEvents: 'none',
                    }}
                />

                {/* Tier badge */}
                {product.tier && (
                    <div style={{ position: 'absolute', top: 8, left: 8 }}>
                        {product.tier === 'trending' && <span className="font-mono text-xs text-green-400">🔥 HOT</span>}
                        {product.tier === 'rising' && <span className="font-mono text-xs text-yellow-400">↑ RISING</span>}
                        {product.tier === 'curated' && <span className="font-mono text-xs text-purple-300">✦ INDIE</span>}
                    </div>
                )}

                {/* Discount */}
                {discountPct && (
                    <div style={{ position: 'absolute', top: 8, left: 70 }}>
                        <span className="text-red-400 text-xs">-{discountPct}%</span>
                    </div>
                )}

                {/* Save */}
                <button onClick={handleSave} style={{ position: 'absolute', top: 8, right: 8 }}>
                    ♥
                </button>

                {/* Velocity */}
                {product.velocityLabel && (
                    <div style={{ position: 'absolute', bottom: 8, left: 8 }}>
                        <VelocityBadge label={product.velocityLabel} />
                    </div>
                )}

                {/* Score */}
                {!isCurated && product.trendScore && (
                    <div style={{ position: 'absolute', bottom: 8, right: 8 }}>
                        <ScoreRing score={product.trendScore} size={30} strokeWidth={3} />
                    </div>
                )}
            </div>

            {/* CONTENT */}
            <div style={{ padding: 12 }}>
                <h3>{product.productName}</h3>

                <div>
                    ₹{displayPrice}
                    {discountedPrice && mainPrice > discountedPrice && (
                        <span style={{ textDecoration: 'line-through', marginLeft: 6 }}>
                            ₹{mainPrice}
                        </span>
                    )}
                </div>

                {/* ✅ FIXED SHOP BUTTON */}
                {shopUrl && (
                    <a
                        href={shopUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        onClick={(e) => {
                            e.stopPropagation();
                            trackClick(product.id, product.platform || 'store', source);
                        }}
                    >
                        SHOP →
                    </a>
                )}
            </div>
        </div>
    );
}