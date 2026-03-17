export default function ScoreRing({ score, size = 40, strokeWidth = 4 }) {
  if (score === null || score === undefined) return null;
  
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (score / 100) * circumference;

  return (
    <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="transform -rotate-90">
        <circle
          className="score-ring-bg"
          strokeWidth={strokeWidth}
          r={radius}
          cx={size / 2}
          cy={size / 2}
        />
        <circle
          className="score-ring-fg stroke-lime-400"
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          r={radius}
          cx={size / 2}
          cy={size / 2}
        />
      </svg>
      <span className="absolute font-display font-bold text-white text-sm" style={{ fontSize: size * 0.35 }}>
        {score}
      </span>
    </div>
  );
}
