export default function StatsCard({ icon, label, value }) {
    return (
        <div className="flex items-center justify-between py-3 border-b border-border/50 last:border-b-0">
            <div className="flex items-center gap-3">
                <span style={{ fontSize: 16 }}>{icon}</span>
                <span style={{ fontSize: 12, fontWeight: 500 }} className="text-text2">{label}</span>
            </div>
            <span style={{ fontFamily: 'var(--font-heading)', fontSize: 20, fontWeight: 900 }} className="text-text">
                {value}
            </span>
        </div>
    );
}
