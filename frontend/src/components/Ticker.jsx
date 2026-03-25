const items = [];

export default function Ticker() {
  if (items.length === 0) return null;

  const content = items.map((item, i) => (
    <span key={i} className="font-mono" style={{ whiteSpace: "nowrap" }}>
      {item.name}{" "}
      <span style={{ color: "hsl(80, 80%, 55%)" }}>▲{item.velocity}</span>
      <span style={{ margin: "0 28px", opacity: 0.3 }}>·</span>
    </span>
  ));

  return (
    <div className="ticker-wrap" style={{ height: 36 }}>
      <div
        className="ticker-track"
        style={{
          display: "flex",
          alignItems: "center",
          height: "100%",
          fontSize: 12,
          fontWeight: 500,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          color: "hsl(80, 80%, 65%)",
        }}
      >
        {/* Duplicate content for seamless loop */}
        <div style={{ display: "flex", paddingRight: 28 }}>{content}</div>
        <div style={{ display: "flex", paddingRight: 28 }}>{content}</div>
      </div>
    </div>
  );
}
