import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import { fetchRisingTrends, isBackendOffline } from '../api/client';
import TrendCard from '../components/TrendCard';
import SkeletonCard from '../components/SkeletonCard';

export default function RisingPage() {
    const { searchTerm } = useOutletContext();
    const [trends, setTrends] = useState([]);
    const [loading, setLoading] = useState(true);
    const [offline, setOffline] = useState(false);

    useEffect(() => {
        (async () => {
            const data = await fetchRisingTrends();
            setTrends(data);
            setOffline(isBackendOffline());
            setLoading(false);
        })();
    }, []);

    const filtered = searchTerm
        ? trends.filter(t => t.productName?.toLowerCase().includes(searchTerm.toLowerCase()))
        : trends;

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            {offline && (
                <div className="mb-6 px-4 py-3 rounded-lg text-center anim-fade-in"
                    style={{ fontSize: 12, fontWeight: 600, background: 'var(--color-amber-dim)', color: 'var(--color-amber)', border: '1px solid rgba(255,171,0,0.15)' }}>
                    ⚠️ Backend offline — showing cached data
                </div>
            )}

            <section className="py-12 md:py-16">
                <div className="flex items-center gap-3 mb-5">
                    <div className="w-8 h-px bg-red" />
                    <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                        className="text-red">Early Detection</span>
                </div>
                <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: 'clamp(32px, 5vw, 56px)', fontWeight: 900, letterSpacing: '-1px', lineHeight: 1.1 }}
                    className="text-text mb-4">
                    Rising <em style={{ fontStyle: 'italic', color: 'var(--color-red)' }}>Fast</em> 🚀
                </h1>
                <p style={{ fontSize: 14, lineHeight: 1.7 }} className="text-text2 max-w-lg">
                    Trends with the highest velocity — detected before they hit mass adoption.
                </p>
            </section>

            {/* Detection banner */}
            <section className="rounded-xl mb-6 px-5 py-4"
                style={{ background: 'var(--color-red-dim)', border: '1px solid rgba(255,61,87,0.1)' }}>
                <div className="flex items-center gap-3">
                    <span>⚡</span>
                    <div>
                        <p style={{ fontSize: 12, fontWeight: 600 }} className="text-text">Early Detection Engine Active</p>
                        <p style={{ fontSize: 11 }} className="text-text3 mt-0.5">Velocity threshold: +100% week-over-week</p>
                    </div>
                </div>
            </section>

            <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mb-16">
                {loading
                    ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
                    : filtered.map((trend, i) => <TrendCard key={trend.id} trend={trend} index={i} />)
                }
                {!loading && filtered.length === 0 && (
                    <div className="col-span-full text-center py-20 anim-fade-in">
                        <span className="text-3xl mb-3 block">🌱</span>
                        <p className="text-text3" style={{ fontSize: 14 }}>No rising trends detected right now</p>
                    </div>
                )}
            </section>
        </div>
    );
}
