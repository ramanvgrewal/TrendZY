import { useRef } from 'react';
import ProductCard from './ProductCard';

/**
 * CategoryRow
 * Props:
 *   cfg      — { key, label, emoji, accent, dim }  (from productTypeClassifier)
 *   products — array of product objects
 *   source   — analytics source string
 */
export default function CategoryRow({ cfg, products = [], source = 'cat_row' }) {
    const ref = useRef(null);
    if (!products.length) return null;

    const { label, emoji, accent, dim } = cfg;

    const scroll = (dir) =>
        ref.current?.scrollBy({ left: dir * 560, behavior: 'smooth' });

    return (
        <div style={{ marginBottom: 36 }}>

            {/* ── Header ── */}
            <div
                style={{
                    maxWidth: 1280,
                    margin: '0 auto',
                    padding: '0 16px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: 12,
                }}
            >
                {/* Left: accent bar + emoji + text */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span
              style={{
                  width: 3,
                  height: 40,
                  borderRadius: 3,
                  background: accent,
                  flexShrink: 0,
                  display: 'block',
              }}
          />
                    <span
                        style={{
                            fontSize: 22,
                            lineHeight: 1,
                            filter: 'drop-shadow(0 0 6px rgba(255,255,255,0.1))',
                        }}
                    >
            {emoji}
          </span>
                    <div>
                        <h3
                            className="font-display"
                            style={{
                                fontSize: 18,
                                fontWeight: 800,
                                color: '#f0f0f0',
                                letterSpacing: '0.005em',
                                lineHeight: 1.1,
                                margin: 0,
                            }}
                        >
                            {label}
                        </h3>
                        <p
                            className="font-mono"
                            style={{
                                fontSize: 9,
                                fontWeight: 700,
                                letterSpacing: '0.18em',
                                textTransform: 'uppercase',
                                color: accent,
                                margin: 0,
                                marginTop: 3,
                                opacity: 0.85,
                            }}
                        >
                            {products.length} item{products.length !== 1 ? 's' : ''}
                        </p>
                    </div>
                </div>

                {/* Right: scroll arrows */}
                <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <span
              className="font-mono"
              style={{
                  fontSize: 9,
                  fontWeight: 700,
                  letterSpacing: '0.1em',
                  textTransform: 'uppercase',
                  padding: '3px 9px',
                  borderRadius: 4,
                  background: dim,
                  color: accent,
                  border: `1px solid ${accent}28`,
                  marginRight: 6,
              }}
          >
            SCROLL →
          </span>
                    {[-1, 1].map((dir) => (
                        <button
                            key={dir}
                            onClick={() => scroll(dir)}
                            style={{
                                width: 30,
                                height: 30,
                                borderRadius: '50%',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                background: dim,
                                border: `1px solid rgba(255,255,255,0.07)`,
                                color: 'rgba(255,255,255,0.45)',
                                cursor: 'pointer',
                                fontSize: 13,
                                lineHeight: 1,
                                transition: 'all 0.18s',
                                flexShrink: 0,
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.borderColor = `${accent}55`;
                                e.currentTarget.style.color = accent;
                                e.currentTarget.style.background = `${accent}14`;
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.07)';
                                e.currentTarget.style.color = 'rgba(255,255,255,0.45)';
                                e.currentTarget.style.background = dim;
                            }}
                        >
                            {dir === -1 ? '←' : '→'}
                        </button>
                    ))}
                </div>
            </div>

            {/* ── Scroll row ── */}
            <div style={{ position: 'relative', overflow: 'hidden' }}>
                <div
                    ref={ref}
                    style={{
                        display: 'flex',
                        gap: 12,
                        overflowX: 'auto',
                        scrollbarWidth: 'none',
                        padding: '4px 16px 12px',
                        maxWidth: 1280,
                        margin: '0 auto',
                        scrollSnapType: 'x mandatory',
                    }}
                >
                    {products.map((p) => (
                        <div
                            key={p.id}
                            style={{
                                flexShrink: 0,
                                width: 210,
                                scrollSnapAlign: 'start',
                            }}
                        >
                            <ProductCard
                                product={p}
                                source={source}
                                accentColor={accent}
                            />
                        </div>
                    ))}
                </div>

                {/* Right fade */}
                <div
                    style={{
                        position: 'absolute',
                        right: 0,
                        top: 0,
                        bottom: 12,
                        width: 64,
                        pointerEvents: 'none',
                        background: 'linear-gradient(to left, hsl(0 0% 4%), transparent)',
                    }}
                />
            </div>
        </div>
    );
}