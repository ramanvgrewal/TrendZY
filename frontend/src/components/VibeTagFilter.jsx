export default function VibeTagFilter({ vibes, activeVibe, onVibeChange }) {
    if (!vibes || vibes.length === 0) return null;
    return (
        <div className="flex items-center gap-3 w-full mb-6 anim-fade-in">
            <div className="flex gap-3 overflow-x-auto scrollbar-hide flex-1 pb-2">
                <button
                    onClick={() => onVibeChange('')}
                    className={`shrink-0 rounded-full px-4 py-1.5 font-body text-sm font-medium transition-colors ${activeVibe === '' ? 'bg-primary text-background' : 'bg-surface-elevated text-muted hover:text-foreground'}`}>
                    All Vibes
                </button>
                {vibes.map((v) => (
                    <button key={v} onClick={() => onVibeChange(v)}
                        className={`shrink-0 rounded-full px-4 py-1.5 font-body text-sm font-medium transition-colors ${activeVibe === v ? 'bg-lime text-[#000]' : 'bg-surface-elevated text-muted hover:text-lime'}`}>
                        #{v}
                    </button>
                ))}
            </div>
        </div>
    );
}
