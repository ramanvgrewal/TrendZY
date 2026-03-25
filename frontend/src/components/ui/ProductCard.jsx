import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { trackClick } from '../../api/affiliate';
import { saveProduct, unsaveProduct } from '../../api/user';
import useAuthStore from '../../store/authStore';
import ScoreRing from './ScoreRing';
import VelocityBadge from './VelocityBadge';

export default function ProductCard({
                                      product,
                                      source = 'home_feed',
                                      accentColor = '#a3e635',
                                    }) {
  const navigate = useNavigate();
  const { isLoggedIn, openAuthModal } = useAuthStore();
  const [isSaved, setIsSaved] = useState(false);

  const productImg = product.image || product.imageUrl || product.primaryImageUrl;
  const validStatus = product.enrichmentStatus === undefined || product.enrichmentStatus === null || product.enrichmentStatus === 'SUCCESS' || product.enrichmentStatus === 'COMPLETED';

  if (!product || !product.shopUrl || !productImg || !validStatus) {
    return null;
  }

  const isCurated = product.platform === 'brand' || product.tier === 'curated';

  /* ── Interaction handlers ── */
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
    if (!isLoggedIn) { openAuthModal(); return; }
    const prev = isSaved;
    setIsSaved(!prev);
    try {
      if (prev) { await unsaveProduct(product.id); toast.success('Removed'); }
      else { await saveProduct(product.id); toast.success('Saved ♥'); }
    } catch { setIsSaved(prev); toast.error('Could not update.'); }
  };

  const displayPrice = product.price || product.estimatedPrice;
  const accent = isCurated ? '#c4b5fd' : accentColor;

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
        {/* ═══ IMAGE ═══ */}
        <div
            style={{
              position: 'relative',
              width: '100%',
              background: '#141414',
              overflow: 'hidden',
              flexShrink: 0,
            }}
        >
          <img
              src={productImg}
              alt={product.productName}
              draggable={false}
              onError={(e) => { e.target.src = "/fallback.png"; }}
              className="w-full h-64 object-contain rounded-xl group-hover:scale-105"
              style={{
                transition: 'transform 480ms ease',
                padding: '12px',
              }}
          />

          {/* Bottom gradient for readability */}
          <div
              style={{
                position: 'absolute',
                inset: 0,
                background: 'linear-gradient(to top, rgba(0,0,0,0.65) 0%, transparent 45%)',
                pointerEvents: 'none',
              }}
          />

          {/* ── Tier badge (top-left) ── */}
          {product.tier && (
              <div style={{ position: 'absolute', top: 8, left: 8 }}>
                {product.tier === 'trending' && (
                    <span
                        className="font-mono"
                        style={{
                          fontSize: 8, fontWeight: 700, letterSpacing: '0.14em',
                          textTransform: 'uppercase', padding: '2px 7px', borderRadius: 4,
                          background: 'rgba(163,230,53,0.14)',
                          color: '#a3e635',
                          border: '1px solid rgba(163,230,53,0.28)',
                          backdropFilter: 'blur(6px)',
                        }}
                    >
                🔥 HOT
              </span>
                )}
                {product.tier === 'rising' && (
                    <span
                        className="font-mono"
                        style={{
                          fontSize: 8, fontWeight: 700, letterSpacing: '0.14em',
                          textTransform: 'uppercase', padding: '2px 7px', borderRadius: 4,
                          background: 'rgba(251,191,36,0.14)',
                          color: '#fbbf24',
                          border: '1px solid rgba(251,191,36,0.28)',
                          backdropFilter: 'blur(6px)',
                        }}
                    >
                ↑ RISING
              </span>
                )}
                {product.tier === 'curated' && (
                    <span
                        className="font-mono"
                        style={{
                          fontSize: 8, fontWeight: 700, letterSpacing: '0.14em',
                          textTransform: 'uppercase', padding: '2px 7px', borderRadius: 4,
                          background: 'rgba(196,181,253,0.14)',
                          color: '#c4b5fd',
                          border: '1px solid rgba(196,181,253,0.28)',
                          backdropFilter: 'blur(6px)',
                        }}
                    >
                ✦ INDIE
              </span>
                )}
              </div>
          )}

          {/* ── Save button (top-right) ── */}
          <button
              onClick={handleSave}
              style={{
                position: 'absolute', top: 8, right: 8,
                width: 28, height: 28, borderRadius: '50%',
                background: 'rgba(0,0,0,0.55)',
                backdropFilter: 'blur(6px)',
                border: `1px solid ${isSaved ? 'rgba(239,68,68,0.5)' : 'rgba(255,255,255,0.15)'}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(0,0,0,0.75)')}
              onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(0,0,0,0.55)')}
          >
            <svg width="12" height="12" viewBox="0 0 24 24"
                 fill={isSaved ? '#ef4444' : 'none'}
                 stroke={isSaved ? '#ef4444' : 'rgba(255,255,255,0.8)'}
                 strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
            >
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
          </button>

          {/* ── Velocity badge (bottom-left) ── */}
          {product.velocityLabel && (
              <div style={{ position: 'absolute', bottom: 8, left: 8 }}>
                <VelocityBadge label={product.velocityLabel} />
              </div>
          )}

          {/* ── Score ring (bottom-right) ── */}
          {!isCurated && product.trendScore && (
              <div style={{ position: 'absolute', bottom: 8, right: 8 }}>
                <ScoreRing score={product.trendScore} size={30} strokeWidth={3} />
              </div>
          )}
        </div>

        {/* ═══ CONTENT ═══ */}
        <div
            style={{
              padding: '11px 13px 13px',
              display: 'flex',
              flexDirection: 'column',
              flex: 1,
            }}
        >
          {/* Brand + category micro-label */}
          <div
              style={{
                display: 'flex', alignItems: 'center', gap: 5, marginBottom: 4,
                minHeight: 14,
              }}
          >
            {product.brandName && (
                <span
                    className="font-mono"
                    style={{
                      fontSize: 9, fontWeight: 700, textTransform: 'uppercase',
                      letterSpacing: '0.1em', color: 'rgba(255,255,255,0.45)',
                    }}
                >
              {product.brandName}
            </span>
            )}
            {product.brandName && product.category && (
                <span style={{ fontSize: 8, color: 'rgba(255,255,255,0.2)' }}>·</span>
            )}
            {product.category && (
                <span
                    className="font-mono"
                    style={{
                      fontSize: 9, textTransform: 'uppercase',
                      letterSpacing: '0.08em', color: 'rgba(255,255,255,0.3)',
                    }}
                >
              {product.category}
            </span>
            )}
          </div>

          {/* Product name */}
          <h3
              className="font-display"
              style={{
                fontSize: 14,
                fontWeight: 700,
                color: '#f5f5f5',
                lineHeight: 1.3,
                marginBottom: 7,
              }}
          >
            {product.productName}
          </h3>

          {/* Vibe tags */}
          {product.vibeTags && product.vibeTags.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 9 }}>
                {product.vibeTags.slice(0, 2).map((v, i) => (
                    <span
                        key={i}
                        onClick={(e) => handleVibeClick(e, v)}
                        className="font-mono"
                        style={{
                          fontSize: 9, padding: '2px 6px', borderRadius: 4, cursor: 'pointer',
                          background: `${accent}12`,
                          color: accent,
                          border: `1px solid ${accent}28`,
                          transition: 'opacity 0.15s',
                        }}
                        onMouseEnter={(e) => (e.target.style.opacity = 0.7)}
                        onMouseLeave={(e) => (e.target.style.opacity = 1)}
                    >
                {v}
              </span>
                ))}
              </div>
          )}

          {/* Spacer */}
          <div style={{ flex: 1 }} />

          {/* Price + Buy */}
          <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderTop: '1px solid rgba(255,255,255,0.06)',
                paddingTop: 9,
                marginTop: 4,
              }}
          >
          <span
              className="font-display"
              style={{ fontSize: 16, fontWeight: 700, color: '#f5f5f5' }}
          >
            {displayPrice
                ? `₹${typeof displayPrice === 'number'
                    ? displayPrice.toLocaleString('en-IN')
                    : displayPrice}`
                : 'TBA'}
          </span>

            {product.shopUrl && (
                <a
                    href={product.shopUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => { e.stopPropagation(); trackClick(product.id, product.platform || 'store', source); }}
                    className="font-mono"
                    style={{
                      display: 'inline-block',
                      fontSize: 10, fontWeight: 700,
                      padding: '5px 11px', borderRadius: 7,
                      background: accent,
                      color: '#000',
                      textTransform: 'uppercase', letterSpacing: '0.05em',
                      textDecoration: 'none',
                      transition: 'filter 0.15s, transform 0.12s',
                    }}
                    onMouseEnter={(e) => { e.target.style.filter = 'brightness(1.12)'; e.target.style.transform = 'scale(1.04)'; }}
                    onMouseLeave={(e) => { e.target.style.filter = ''; e.target.style.transform = ''; }}
                >
                  SHOP ON {product.platform ? product.platform.toUpperCase() : 'STORE'}
                </a>
            )}
          </div>
        </div>
      </div>
  );
}