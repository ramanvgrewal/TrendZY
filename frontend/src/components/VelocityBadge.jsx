export default function VelocityBadge({ velocity }) {
  if (!velocity) return null;
  return (
    <span
      className="font-mono"
      style={{
        fontSize: 11,
        fontWeight: 500,
        letterSpacing: "0.04em",
        padding: "3px 10px",
        borderRadius: 6,
        background: "hsl(80 80% 55% / 0.12)",
        color: "hsl(80, 80%, 55%)",
        whiteSpace: "nowrap",
      }}
    >
      ▲ {velocity}
    </span>
  );
}
