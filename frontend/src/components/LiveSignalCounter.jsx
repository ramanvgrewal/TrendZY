export default function LiveSignalCounter() {
  const stats = [
    { label: 'SIGNALS TODAY', value: '2,847' },
    { label: 'TRENDS DETECTED', value: '156' },
    { label: 'SUBREDDITS', value: '25+' },
    { label: 'LAST SCAN', value: '4 min ago' },
  ];

  return (
    <div
      className="anim-fade-in"
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 24,
        flexWrap: 'wrap',
        padding: '14px 0',
        borderTop: '1px solid hsl(0 0% 12%)',
        borderBottom: '1px solid hsl(0 0% 12%)',
        marginBottom: 40,
      }}
    >
      {/* Live indicator */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div className="live-dot" />
        <span
          className="font-mono"
          style={{
            fontSize: 11,
            fontWeight: 500,
            letterSpacing: '0.12em',
            color: 'hsl(80, 80%, 55%)',
          }}
        >
          LIVE
        </span>
      </div>

      {/* Stats */}
      {stats.map((stat) => (
        <div
          key={stat.label}
          className="font-mono"
          style={{
            fontSize: 11,
            fontWeight: 500,
            letterSpacing: '0.06em',
            color: 'hsl(0 0% 50%)',
          }}
        >
          {stat.label}:{' '}
          <span style={{ color: 'hsl(0 0% 85%)' }}>{stat.value}</span>
        </div>
      ))}
    </div>
  );
}
