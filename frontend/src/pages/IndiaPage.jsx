import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import { fetchTrends, isBackendOffline } from '../api/client';
import { getSessionId } from '../utils/session';
import TrendCard from '../components/TrendCard';
import SkeletonCard from '../components/SkeletonCard';
import FilterBar from '../components/FilterBar';

export default function IndiaPage() {
    const { searchTerm } = useOutletContext();
    const [trends, setTrends] = useState([]);
    const [loading, setLoading] = useState(true);
    const [offline, setOffline] = useState(false);

    useEffect(() => {
        (async () => {
            const data = await fetchTrends({ indiaRelevant: true, sessionId: getSessionId() });
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
                    <div className="w-8 h-px bg-amber" />
                    <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                        className="text-amber">India Market</span>
                </div>
                <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: 'clamp(32px, 5vw, 56px)', fontWeight: 900, letterSpacing: '-1px', lineHeight: 1.1 }}
                    className="text-text mb-4">
                    Trending in <em style={{ fontStyle: 'italic', color: 'var(--color-amber)' }}>India</em> 🇮🇳
                </h1>
                <p style={{ fontSize: 14, lineHeight: 1.7 }} className="text-text2 max-w-lg">
                    Curated trends specifically relevant to Indian consumers, brands, and markets.
                </p>
            </section>

            <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mb-16">
                {loading
                    ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
                    : filtered.map((trend, i) => <TrendCard key={trend.id} trend={trend} index={i} />)
                }
                {!loading && filtered.length === 0 && (
                    <div className="col-span-full text-center py-20 anim-fade-in">
                        <span className="text-3xl mb-3 block">🇮🇳</span>
                        <p className="text-text3" style={{ fontSize: 14 }}>No India-relevant trends found</p>
                    </div>
                )}
            </section>
        </div>
    );
}
