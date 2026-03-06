export default function SkeletonCard() {
    return (
        <div style={{ background: 'var(--color-card)', border: '1px solid var(--color-border)', borderRadius: 16, padding: 22 }}
            className="space-y-4">
            <div className="flex justify-between items-center">
                <div className="h-5 w-24 rounded anim-shimmer" />
                <div className="h-7 w-10 rounded anim-shimmer" />
            </div>
            <div className="h-5 w-3/4 rounded anim-shimmer" />
            <div className="space-y-2">
                <div className="h-3 w-full rounded anim-shimmer" />
                <div className="h-3 w-2/3 rounded anim-shimmer" />
            </div>
            <div className="h-0.5 w-full rounded-full anim-shimmer" />
            <div className="flex gap-1">
                <div className="h-5 w-14 rounded anim-shimmer" />
                <div className="h-5 w-14 rounded anim-shimmer" />
            </div>
            <div className="h-3 w-28 rounded anim-shimmer" />
            <div className="flex gap-2">
                <div className="h-9 flex-1 rounded-lg anim-shimmer" />
                <div className="h-9 flex-1 rounded-lg anim-shimmer" />
            </div>
        </div>
    );
}
