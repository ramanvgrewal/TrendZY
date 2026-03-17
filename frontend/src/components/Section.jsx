import { useRef } from 'react';
import TrendCard from './TrendCard';

export default function Section({ title, emoji, trends, type = 'normal' }) {
    const scrollRef = useRef(null);

    const scroll = (direction) => {
        if (scrollRef.current) {
            const amount = direction === 'left' ? -350 : 350;
            scrollRef.current.scrollBy({ left: amount, behavior: 'smooth' });
        }
    };

    if (!trends || trends.length === 0) return null;

    return (
        <section className="mb-12 anim-fade-up">
            <div className="flex items-end justify-between mb-5">
                <div className="flex items-center gap-3">
                    {emoji && <span className="text-2xl">{emoji}</span>}
                    <h2 className={`font-display font-bold text-foreground ${type === 'featured' ? 'text-3xl lg:text-4xl text-red' : 'text-2xl lg:text-3xl'}`}>
                        {title}
                    </h2>
                </div>
                {/* Scroll buttons */}
                <div className="hidden sm:flex gap-2">
                    <button onClick={() => scroll('left')} className="p-2 rounded-full bg-surface-elevated hover:bg-card transition flex items-center justify-center w-10 h-10 border border-border text-foreground hover:text-primary">
                        &larr;
                    </button>
                    <button onClick={() => scroll('right')} className="p-2 rounded-full bg-surface-elevated hover:bg-card transition flex items-center justify-center w-10 h-10 border border-border text-foreground hover:text-primary">
                        &rarr;
                    </button>
                </div>
            </div>

            <div
                ref={scrollRef}
                className="flex gap-5 overflow-x-auto scrollbar-hide pb-4 -mx-4 px-4 sm:mx-0 sm:px-0"
                style={{ scrollSnapType: 'x mandatory' }}
            >
                {trends.map((trend, i) => (
                    <div key={trend.id} className="w-[85vw] sm:w-[350px] shrink-0" style={{ scrollSnapAlign: 'start' }}>
                        <TrendCard trend={trend} index={i} />
                    </div>
                ))}
            </div>
        </section>
    );
}
