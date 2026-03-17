export default function ScoreRing({ score, size = 56 }) {
  const radius = (size - 8) / 2;
  const circumference = 2 * Math.PI * radius;
  const pct = Math.min(Math.max(score, 0), 100);
  const offset = circumference - (pct / 100) * circumference;

  let color;
  if (score >= 85)      color = "hsl(80, 80%, 55%)";   // lime
  else if (score >= 70) color = "hsl(43, 96%, 56%)";   // amber
  else                  color = "hsl(25, 95%, 53%)";   // orange

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <circle
        className="score-ring-bg"
        cx={size / 2} cy={size / 2} r={radius}
        strokeWidth="4"
      />
      <circle
        className="score-ring-fg"
        cx={size / 2} cy={size / 2} r={radius}
        strokeWidth="4"
        stroke={color}
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
      <text
        x="50%" y="50%"
        dominantBaseline="central" textAnchor="middle"
        fill={color}
        style={{ fontFamily: '"DM Mono", monospace', fontSize: size * 0.32, fontWeight: 500 }}
      >
        {score}
      </text>
    </svg>
  );
}
