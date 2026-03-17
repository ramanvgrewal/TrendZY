import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';

const links = [
  { to: '/',                label: 'Trending' },
  { to: '/rising',          label: 'Rising' },
  { to: '/only-on-trendzy', label: 'Only on TrendZY' },
  { to: '/admin/curated',   label: 'Brands' },
];

export default function Navbar({ searchTerm, onSearch }) {
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 10);
    window.addEventListener('scroll', handler, { passive: true });
    return () => window.removeEventListener('scroll', handler);
  }, []);

  return (
    <header
      className="sticky top-0 z-50 transition-all duration-300"
      style={{
        background: scrolled ? 'hsl(0 0% 4% / 0.85)' : 'hsl(0 0% 4% / 0.95)',
        backdropFilter: scrolled ? 'blur(16px)' : 'none',
        borderBottom: scrolled
          ? '1px solid hsl(0 0% 16% / 0.6)'
          : '1px solid transparent',
      }}
    >
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        {/* ── Logo ── */}
        <Link to="/" className="flex items-center gap-3" style={{ textDecoration: 'none' }}>
          <div
            style={{
              width: 32, height: 32, borderRadius: 6,
              background: 'hsl(80, 80%, 55%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: '"Space Grotesk", sans-serif',
              fontSize: 16, fontWeight: 700, color: '#0a0a0a',
            }}
          >
            T
          </div>
          <span
            className="font-display"
            style={{ fontSize: 18, fontWeight: 700, letterSpacing: '0.14em', color: 'hsl(0 0% 96%)' }}
          >
            TRENDZY
          </span>
        </Link>

        {/* ── Nav Links ── */}
        <nav className="hidden items-center gap-1 sm:flex">
          {links.map((link) => {
            const active = location.pathname === link.to;
            return (
              <Link
                key={link.to}
                to={link.to}
                className="font-mono"
                style={{
                  fontSize: 12,
                  fontWeight: 500,
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                  padding: '6px 14px',
                  borderRadius: 8,
                  color: active ? 'hsl(80, 80%, 55%)' : 'hsl(0 0% 45%)',
                  background: active ? 'hsl(80 80% 55% / 0.08)' : 'transparent',
                  textDecoration: 'none',
                  transition: 'color 0.2s, background 0.2s',
                }}
                onMouseEnter={(e) => { if (!active) e.target.style.color = 'hsl(0 0% 96%)'; }}
                onMouseLeave={(e) => { if (!active) e.target.style.color = 'hsl(0 0% 45%)'; }}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* ── Right: Search + Avatar ── */}
        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="flex items-center">
            {searchOpen && (
              <input
                autoFocus
                value={searchTerm}
                onChange={(e) => onSearch(e.target.value)}
                placeholder="Search trends…"
                className="font-mono"
                style={{
                  width: 200,
                  fontSize: 12,
                  padding: '7px 12px',
                  borderRadius: 8,
                  border: '1px solid hsl(0 0% 20%)',
                  background: 'hsl(0 0% 9%)',
                  color: 'hsl(0 0% 96%)',
                  outline: 'none',
                  marginRight: 8,
                  transition: 'width 0.3s ease',
                }}
              />
            )}
            <button
              onClick={() => setSearchOpen(!searchOpen)}
              style={{
                width: 36, height: 36, borderRadius: 8,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: 'transparent',
                border: '1px solid hsl(0 0% 20%)',
                color: 'hsl(0 0% 50%)',
                cursor: 'pointer',
                transition: 'border-color 0.2s, color 0.2s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.borderColor = 'hsl(80 80% 55% / 0.4)'; e.currentTarget.style.color = 'hsl(80, 80%, 55%)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.borderColor = 'hsl(0 0% 20%)'; e.currentTarget.style.color = 'hsl(0 0% 50%)'; }}
              aria-label="Search"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
            </button>
          </div>

          {/* Avatar */}
          <div
            style={{
              width: 32, height: 32, borderRadius: '50%',
              background: 'hsl(0 0% 16%)',
              border: '1px solid hsl(0 0% 22%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 13, color: 'hsl(0 0% 50%)',
            }}
          >
            👤
          </div>
        </div>
      </div>
    </header>
  );
}
