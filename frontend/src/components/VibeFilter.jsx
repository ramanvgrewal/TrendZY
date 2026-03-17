const defaultVibes = ['All', '#GenZ', '#Y2K', '#Streetwear', '#Cottagecore', '#CleanGirl', '#Indie', '#Gorpcore', '#Dopamine'];

export default function VibeFilter({ activeVibe, onVibeChange }) {
  return (
    <div
      className="scrollbar-hide anim-fade-in"
      style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}
    >
      {defaultVibes.map((vibe) => {
        const value = vibe === 'All' ? '' : vibe.replace('#', '');
        const active = (vibe === 'All' && activeVibe === '') || activeVibe === value;
        return (
          <button
            key={vibe}
            onClick={() => onVibeChange(value)}
            className="font-mono"
            style={{
              flexShrink: 0,
              fontSize: 11,
              fontWeight: 500,
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
              padding: '7px 18px',
              borderRadius: 20,
              cursor: 'pointer',
              transition: 'all 0.2s',
              border: active
                ? '1px solid hsl(80, 80%, 55%)'
                : '1px solid hsl(0 0% 20%)',
              background: active
                ? 'hsl(80, 80%, 55%)'
                : 'transparent',
              color: active
                ? '#0a0a0a'
                : 'hsl(0 0% 50%)',
            }}
            onMouseEnter={(e) => { if (!active) { e.target.style.borderColor = 'hsl(80 80% 55% / 0.4)'; e.target.style.color = 'hsl(0 0% 80%)'; }}}
            onMouseLeave={(e) => { if (!active) { e.target.style.borderColor = 'hsl(0 0% 20%)'; e.target.style.color = 'hsl(0 0% 50%)'; }}}
          >
            {vibe}
          </button>
        );
      })}
    </div>
  );
}
