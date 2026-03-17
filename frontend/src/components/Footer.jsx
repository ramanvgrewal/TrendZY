export default function Footer() {
  return (
    <footer
      style={{
        textAlign: 'center',
        padding: '48px 24px 32px',
        borderTop: '1px solid hsl(0 0% 12%)',
        marginTop: 60,
      }}
    >
      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, marginBottom: 12 }}>
        <div
          style={{
            width: 28, height: 28, borderRadius: 6,
            background: 'hsl(80, 80%, 55%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: '"Space Grotesk", sans-serif',
            fontSize: 14, fontWeight: 700, color: '#0a0a0a',
          }}
        >
          T
        </div>
        <span
          className="font-display"
          style={{ fontSize: 16, fontWeight: 700, letterSpacing: '0.14em', color: 'hsl(0 0% 96%)' }}
        >
          TRENDZY
        </span>
      </div>

      <p
        className="font-mono"
        style={{ fontSize: 12, color: 'hsl(0 0% 40%)', letterSpacing: '0.04em', marginBottom: 8 }}
      >
        Trend Intelligence for Indian Gen-Z
      </p>
      <p
        className="font-mono"
        style={{ fontSize: 11, color: 'hsl(0 0% 28%)' }}
      >
        © {new Date().getFullYear()} TrendZY. All rights reserved.
      </p>
    </footer>
  );
}
