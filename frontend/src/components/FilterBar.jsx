import { useState } from 'react';

export default function FilterBar({ filters, activeFilter, onFilterChange }) {
    const [sortOpen, setSortOpen] = useState(false);

    return (
        <div className="flex items-center gap-3">
            <div className="flex gap-2 overflow-x-auto scrollbar-hide flex-1">
                {filters.map((f) => (
                    <button key={f.value} onClick={() => onFilterChange(f.value)}
                        className={`filter-pill ${activeFilter === f.value ? 'active' : ''}`}>
                        {f.label}
                    </button>
                ))}
            </div>

            {/* Sort dropdown */}
            <div className="relative shrink-0">
                <button onClick={() => setSortOpen(!sortOpen)}
                    className="filter-pill flex items-center gap-1.5">
                    Sort
                    <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                </button>
                {sortOpen && (
                    <div className="absolute right-0 top-full mt-1 w-40 glass-card p-1.5 z-10 shadow-xl"
                        style={{ background: 'var(--color-card2)', border: '1px solid var(--color-border2)' }}>
                        {['Score ↓', 'Newest', 'Mentions ↓'].map((opt) => (
                            <button key={opt} onClick={() => setSortOpen(false)}
                                style={{ fontSize: 12 }}
                                className="w-full text-left px-3 py-2 rounded-lg text-text2 hover:text-text hover:bg-card transition-colors cursor-pointer">
                                {opt}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
