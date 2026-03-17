import { useState, useEffect } from 'react';
import { fetchCuratedProducts, fetchCuratedCategories } from '../api/client';
import CuratedCard from '../components/CuratedCard';

const VIBE_OPTIONS = [
    'Streetwear', 'Y2K', '90sVibes', 'Sustainable', 'Handmade',
    'IndieIndia', 'SmallBusiness', 'GenZ', 'BudgetFit', 'LuxuryPick', 'Limited'
];

export default function OnlyOnTrendzy() {
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [activeCategory, setActiveCategory] = useState('');
    const [activeVibe, setActiveVibe] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCuratedCategories().then(setCategories);
    }, []);

    useEffect(() => {
        setLoading(true);
        fetchCuratedProducts(activeCategory || undefined).then(data => {
            setProducts(data);
            setLoading(false);
        });
    }, [activeCategory]);

    const filtered = activeVibe
        ? products.filter(p => p.vibeTags && p.vibeTags.includes(activeVibe))
        : products;

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
            {/* Hero header */}
            <section className="py-16 text-center anim-fade-up">
                <div className="flex items-center justify-center gap-3 mb-4">
                    <div className="w-10 h-px" style={{ background: 'var(--color-amber)' }} />
                    <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase', color: 'var(--color-amber)' }}>
                        Curated Collection
                    </span>
                    <div className="w-10 h-px" style={{ background: 'var(--color-amber)' }} />
                </div>
                <h1 style={{ fontFamily: 'Playfair Display, serif', fontSize: 'clamp(36px, 5vw, 64px)', fontWeight: 800, letterSpacing: '-1px', lineHeight: 1.1 }}
                    className="text-text mb-4">
                    <span style={{ color: 'var(--color-amber)' }}>✦</span> Only On TrendZY
                </h1>
                <p style={{ fontSize: 16, lineHeight: 1.7 }} className="text-text2 max-w-lg mx-auto">
                    Hidden gems from indie brands — handpicked products you won't find on Amazon or Myntra.
                </p>
            </section>

            {/* Category filter pills */}
            <div className="flex flex-wrap items-center gap-2 mb-6">
                <button
                    onClick={() => setActiveCategory('')}
                    className={`curated-filter-pill ${activeCategory === '' ? 'active' : ''}`}
                >
                    All
                </button>
                {categories.map(cat => (
                    <button
                        key={cat}
                        onClick={() => setActiveCategory(cat)}
                        className={`curated-filter-pill ${activeCategory === cat ? 'active' : ''}`}
                    >
                        {cat}
                    </button>
                ))}
            </div>

            {/* Vibe tag filter bar */}
            <div className="flex flex-wrap items-center gap-2 mb-10">
                <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase' }} className="text-text3 mr-2">
                    Vibes:
                </span>
                {activeVibe && (
                    <button
                        onClick={() => setActiveVibe('')}
                        className="curated-vibe-pill active"
                    >
                        ✕ Clear
                    </button>
                )}
                {VIBE_OPTIONS.map(vibe => (
                    <button
                        key={vibe}
                        onClick={() => setActiveVibe(activeVibe === vibe ? '' : vibe)}
                        className={`curated-vibe-pill ${activeVibe === vibe ? 'active' : ''}`}
                    >
                        #{vibe}
                    </button>
                ))}
            </div>

            {/* Product grid */}
            {loading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {Array.from({ length: 6 }).map((_, i) => (
                        <div key={i} className="rounded-xl anim-shimmer" style={{ height: 420 }} />
                    ))}
                </div>
            ) : filtered.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 anim-fade-up">
                    {filtered.map(p => (
                        <CuratedCard key={p.id} product={p} />
                    ))}
                </div>
            ) : (
                <div className="text-center py-20 anim-fade-in">
                    <span className="text-5xl mb-4 block">✦</span>
                    <p style={{ fontFamily: 'Playfair Display, serif', fontSize: 22, fontWeight: 700 }} className="text-text mb-2">
                        No products found
                    </p>
                    <p className="font-body text-sm text-text2">
                        {activeCategory || activeVibe
                            ? 'Try adjusting your filters.'
                            : 'Curated products are coming soon — stay tuned!'}
                    </p>
                </div>
            )}
        </div>
    );
}
