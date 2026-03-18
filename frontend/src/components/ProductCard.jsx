import ScoreRing from './ScoreRing';
import VelocityBadge from './VelocityBadge';

export default function ProductCard({ product, variant = 'featured' }) {
  const isFeatured = variant === 'featured';
  const maxWidth = isFeatured ? 320 : 220;
  const imgRatio = isFeatured ? '16/11' : '4/3';

  return (
    <article
      className="card-base card-hover group product-card"
      style={{
        width: '100%',
        maxWidth,
        cursor: 'pointer',
        transition: 'transform 280ms ease, box-shadow 280ms ease, border-color 280ms ease',
      }}
    >
      {/* ── Image ── */}
      <div style={{ position: 'relative', width: '100%', aspectRatio: imgRatio, overflow: 'hidden' }}>
        <img
          src={product.image}
          alt={product.name}
          loading="lazy"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            transition: 'transform 0.4s ease',
          }}
          onMouseEnter={(e) => (e.target.style.transform = 'scale(1.06)')}
          onMouseLeave={(e) => (e.target.style.transform = 'scale(1)')}
        />
        {/* Gradient overlay */}
        <div
          style={{
            position: 'absolute', inset: 0,
            background: 'linear-gradient(to top, hsl(0 0% 4% / 0.7) 0%, transparent 50%)',
          }}
        />

        {/* Score ring (featured only) */}
        {isFeatured && product.score !== null && (
          <div style={{ position: 'absolute', top: 12, right: 12 }}>
            <ScoreRing score={product.score} size={52} />
          </div>
        )}

        {/* Tier label for rising */}
        {product.tier === 'rising' && (
          <span
            className="font-mono"
            style={{
              position: 'absolute', top: 12, left: 12,
              fontSize: 10, fontWeight: 500, letterSpacing: '0.08em',
              textTransform: 'uppercase',
              padding: '3px 10px', borderRadius: 5,
              background: 'hsl(43 96% 56% / 0.15)',
              color: 'hsl(43, 96%, 56%)',
              border: '1px solid hsl(43 96% 56% / 0.3)',
            }}
          >
            RISING EARLY
          </span>
        )}

        {/* Indie badge for curated */}
        {product.tier === 'curated' && (
          <span
            className="font-mono"
            style={{
              position: 'absolute', top: 12, left: 12,
              fontSize: 10, fontWeight: 500, letterSpacing: '0.08em',
              textTransform: 'uppercase',
              padding: '3px 10px', borderRadius: 5,
              background: 'hsl(261 78% 86% / 0.15)',
              color: 'hsl(261, 78%, 86%)',
              border: '1px solid hsl(261 78% 86% / 0.3)',
            }}
          >
            INDIE
          </span>
        )}
      </div>

      {/* ── Content ── */}
      <div style={{ padding: isFeatured ? '16px 18px 18px' : '12px 14px 16px' }}>
        {/* Category label */}
        <span
          className="font-mono"
          style={{
            fontSize: 10,
            fontWeight: 500,
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
            color: 'hsl(0 0% 45%)',
            marginBottom: 6,
            display: 'block',
          }}
        >
          {product.category}
        </span>

        {/* Name */}
        <h3
          className="font-display"
          style={{
            fontSize: isFeatured ? 17 : 14,
            fontWeight: 700,
            color: 'hsl(0 0% 96%)',
            lineHeight: 1.3,
            marginBottom: 8,
          }}
        >
          {product.name}
        </h3>

        {/* Brand for curated */}
        {product.tier === 'curated' && product.brand && (
          <p
            className="font-mono"
            style={{
              fontSize: 11,
              fontWeight: 500,
              letterSpacing: '0.08em',
              color: 'hsl(261, 78%, 86%)',
              marginBottom: 8,
            }}
          >
            {product.brand}
          </p>
        )}

        {/* Velocity Badge (not for curated) */}
        {product.velocity && (
          <div style={{ marginBottom: 10 }}>
            <VelocityBadge velocity={product.velocity} />
          </div>
        )}

        {/* Tags */}
        {product.tags && product.tags.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
            {product.tags.slice(0, isFeatured ? 3 : 2).map((tag) => (
              <span
                key={tag}
                className="font-mono"
                style={{
                  fontSize: 10,
                  fontWeight: 500,
                  padding: '2px 8px',
                  borderRadius: 4,
                  background: 'hsl(0 0% 12%)',
                  color: 'hsl(0 0% 55%)',
                  border: '1px solid hsl(0 0% 18%)',
                }}
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        {/* Price + BUY */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto' }}>
          <span
            className="font-display"
            style={{ fontSize: isFeatured ? 18 : 15, fontWeight: 700, color: 'hsl(0 0% 96%)' }}
          >
            {product.price}
          </span>
          <a
            href="https://amazon.in"
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="font-mono"
            style={{
              fontSize: 11,
              fontWeight: 500,
              letterSpacing: '0.06em',
              padding: '6px 14px',
              borderRadius: 8,
              background: 'hsl(80, 80%, 55%)',
              color: '#0a0a0a',
              textDecoration: 'none',
              transition: 'filter 0.2s, transform 0.2s',
            }}
            onMouseEnter={(e) => { e.target.style.filter = 'brightness(1.1)'; e.target.style.transform = 'translateY(-1px)'; }}
            onMouseLeave={(e) => { e.target.style.filter = 'none'; e.target.style.transform = 'none'; }}
          >
            BUY →
          </a>
        </div>
      </div>
    </article>
  );
}
