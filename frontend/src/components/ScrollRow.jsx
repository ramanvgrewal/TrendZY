import { useRef } from 'react';

const badgeColors = {
  lime:   { bg: 'hsl(80 80% 55% / 0.12)',  color: 'hsl(80, 80%, 55%)',  border: 'hsl(80 80% 55% / 0.3)' },
  amber:  { bg: 'hsl(43 96% 56% / 0.12)',  color: 'hsl(43, 96%, 56%)',  border: 'hsl(43 96% 56% / 0.3)' },
  violet: { bg: 'hsl(261 78% 86% / 0.12)', color: 'hsl(261, 78%, 86%)', border: 'hsl(261 78% 86% / 0.3)' },
};

export default function ScrollRow({ badge, badgeColor = 'lime', title, subtitle, children }) {
  const scrollRef = useRef(null);
  const c = badgeColors[badgeColor] || badgeColors.lime;

  const scroll = (dir) => {
    if (!scrollRef.current) return;
    const amount = dir === 'left' ? -360 : 360;
    scrollRef.current.scrollBy({ left: amount, behavior: 'smooth' });
  };

  return (
    <section className="anim-fade-up" style={{ marginBottom: 48 }}>
      {/* ── Header ── */}
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 20 }}>
        <div>
          {/* Badge */}
          {badge && (
            <span
              className="font-mono"
              style={{
                fontSize: 10,
                fontWeight: 500,
                letterSpacing: '0.1em',
                textTransform: 'uppercase',
                padding: '4px 12px',
                borderRadius: 6,
                background: c.bg,
                color: c.color,
                border: `1px solid ${c.border}`,
                marginBottom: 10,
                display: 'inline-block',
              }}
            >
              {badge}
            </span>
          )}
          {/* Title */}
          <h2
            className="font-display"
            style={{
              fontSize: 'clamp(22px, 3vw, 30px)',
              fontWeight: 700,
              color: 'hsl(0 0% 96%)',
              marginTop: badge ? 10 : 0,
              marginBottom: 4,
            }}
          >
            {title}
          </h2>
          {subtitle && (
            <p className="font-mono" style={{ fontSize: 12, color: 'hsl(0 0% 45%)' }}>
              {subtitle}
            </p>
          )}
        </div>

        {/* Scroll arrows */}
        <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
          {['left', 'right'].map((dir) => (
            <button
              key={dir}
              onClick={() => scroll(dir)}
              style={{
                width: 36, height: 36, borderRadius: '50%',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: 'hsl(0 0% 12%)',
                border: '1px solid hsl(0 0% 18%)',
                color: 'hsl(0 0% 60%)',
                cursor: 'pointer',
                fontSize: 14,
                transition: 'border-color 0.2s, color 0.2s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.borderColor = c.border; e.currentTarget.style.color = c.color; }}
              onMouseLeave={(e) => { e.currentTarget.style.borderColor = 'hsl(0 0% 18%)'; e.currentTarget.style.color = 'hsl(0 0% 60%)'; }}
            >
              {dir === 'left' ? '←' : '→'}
            </button>
          ))}
        </div>
      </div>

      {/* ── Scrollable Row ── */}
      <div
        ref={scrollRef}
        className="scrollbar-hide"
        style={{
          display: 'flex',
          gap: 20,
          overflowX: 'auto',
          paddingBottom: 8,
          scrollSnapType: 'x mandatory',
        }}
      >
        {children}
      </div>
    </section>
  );
}
