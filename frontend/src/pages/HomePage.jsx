import { useState, useEffect, useMemo } from 'react';
import { useOutletContext } from 'react-router-dom';
import { fetchTrends, fetchStats, fetchCategories, fetchRisingTrends, isBackendOffline } from '../api/client';
import { getSessionId } from '../utils/session';
import TrendCard from '../components/TrendCard';
import FilterBar from '../components/FilterBar';
import StatsCard from '../components/StatsCard';
import SkeletonCard from '../components/SkeletonCard';

export default function HomePage() {
    const { searchTerm } = useOutletContext();
    const [trends, setTrends] = useState([]);
    const [stats, setStats] = useState(null);
    const [risingCount, setRisingCount] = useState(0);
    const [filters, setFilters] = useState([{ value: '', label: 'All' }]);
    const [loading, setLoading] = useState(true);
    const [activeFilter, setActiveFilter] = useState('');
    const [indiaOnly, setIndiaOnly] = useState(false);
    const [offline, setOffline] = useState(false);

    useEffect(() => {
        (async () => {
            const [statsData, categories, rising] = await Promise.all([
                fetchStats(), fetchCategories(), fetchRisingTrends()
            ]);
            setStats(statsData);
            setRisingCount(rising.length);
            setOffline(isBackendOffline());
            const catFilters = categories.map(c => ({ value: c.toLowerCase(), label: c.charAt(0).toUpperCase() + c.slice(1) }));
            setFilters([{ value: '', label: 'All' }, ...catFilters]);
        })();
    }, []);

    useEffect(() => { loadTrends(); }, [activeFilter, indiaOnly]);

    const loadTrends = async () => {
        setLoading(true);
        const params = { sessionId: getSessionId() };
        if (activeFilter) params.category = activeFilter;
        if (indiaOnly) params.indiaRelevant = true;
        const data = await fetchTrends(params);
        setTrends(data);
        setOffline(isBackendOffline());
        setLoading(false);
    };

    const filteredTrends = useMemo(() => {
        if (!searchTerm) return trends;
        const t = searchTerm.toLowerCase();
        return trends.filter(tr =>
            tr.productName?.toLowerCase().includes(t) ||
            tr.category?.toLowerCase().includes(t) ||
            tr.brandMention?.toLowerCase().includes(t)
        );
    }, [trends, searchTerm]);

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            {/* Offline */}
            {offline && (
                <div className="mb-6 px-4 py-3 rounded-lg text-center anim-fade-in"
                    style={{ fontSize: 12, fontWeight: 600, background: 'var(--color-amber-dim)', color: 'var(--color-amber)', border: '1px solid rgba(255,171,0,0.15)' }}>
                    ⚠️ Backend offline — showing cached data
                </div>
            )}

            {/* ── HERO: 2-col ── */}
            <section className="grid grid-cols-1 lg:grid-cols-5 gap-8 py-12 md:py-20">
                {/* Left — headline */}
                <div className="lg:col-span-3 flex flex-col justify-center">
                    {/* Eyebrow */}
                    <div className="flex items-center gap-3 mb-5">
                        <div className="w-8 h-px bg-lime" />
                        <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                            className="text-lime">AI-Powered Intelligence</span>
                    </div>
                    <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: 'clamp(40px, 6vw, 80px)', fontWeight: 900, letterSpacing: '-2px', lineHeight: 1.05 }}
                        className="text-text mb-5">
                        Discover trends{' '}
                        <em style={{ fontStyle: 'italic', color: 'var(--color-lime)' }}>before</em>
                        <br />they explode
                    </h1>
                    <p style={{ fontSize: 15, lineHeight: 1.7 }} className="text-text2 max-w-md">
                        Real-time signals from Reddit, social media, and AI analysis — curated for the Indian market.
                    </p>
                </div>

                {/* Right — stats panel */}
                <div className="lg:col-span-2">
                    <div className="glass-card overflow-hidden">
                        {/* Top gradient line */}
                        <div className="h-px" style={{ background: 'linear-gradient(90deg, var(--color-lime), transparent)' }} />
                        <div className="p-5">
                            <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                                className="text-text3 mb-3">Live Stats</p>
                            <StatsCard icon="📡" label="Signals Collected" value={stats?.totalSignals?.toLocaleString() ?? '—'} />
                            <StatsCard icon="🔥" label="Active Trends" value={stats?.activeTrends ?? '—'} />
                            <StatsCard icon="🇮🇳" label="India Relevant" value={stats?.indiaRelevantCount ?? '—'} />
                            <StatsCard icon="📊" label="Subreddits Monitored" value={stats?.subredditsMonitored ?? '—'} />
                        </div>
                    </div>
                </div>
            </section>

            {/* ── India Toggle — amber ── */}
            <section className="rounded-xl mb-6 px-5 py-4 flex items-center justify-between gap-4"
                style={{ background: 'var(--color-amber-dim)', border: '1px solid rgba(255,171,0,0.12)' }}>
                <div className="flex items-center gap-3">
                    <span style={{ fontSize: 20 }}>🇮🇳</span>
                    <div>
                        <p style={{ fontFamily: 'var(--font-heading)', fontSize: 14, fontWeight: 700 }} className="text-text">
                            India Trends
                        </p>
                        <p style={{ fontSize: 11 }} className="text-text2 mt-0.5">Show trends relevant to the Indian market</p>
                    </div>
                </div>
                <button onClick={() => setIndiaOnly(!indiaOnly)}
                    className="relative w-11 h-6 rounded-full cursor-pointer transition-colors duration-300"
                    style={{ background: indiaOnly ? 'var(--color-amber)' : 'var(--color-card2)' }}>
                    <span className="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-300"
                        style={{ transform: indiaOnly ? 'translateX(20px)' : 'none' }} />
                </button>
            </section>

            {/* ── Rising banner — red tint ── */}
            {risingCount > 0 && (
                <section className="rounded-xl mb-6 px-5 py-4 anim-fade-up"
                    style={{ background: 'var(--color-red-dim)', border: '1px solid rgba(255,61,87,0.1)' }}>
                    <div className="flex items-center gap-3">
                        <span style={{ fontSize: 18 }}>🔥</span>
                        <div>
                            <p style={{ fontSize: 13, fontWeight: 600 }} className="text-red">
                                {risingCount} trends rising fast this week
                            </p>
                            <p style={{ fontSize: 11 }} className="text-text2 mt-0.5">
                                These trends showed &gt;100% velocity increase week-over-week
                            </p>
                        </div>
                    </div>
                </section>
            )}

            {/* ── Filters ── */}
            <section className="mb-6">
                <FilterBar filters={filters} activeFilter={activeFilter} onFilterChange={setActiveFilter} />
            </section>

            {/* ── Grid ── */}
            <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mb-16">
                {loading
                    ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
                    : filteredTrends.map((trend, i) => <TrendCard key={trend.id} trend={trend} index={i} />)
                }
                {!loading && filteredTrends.length === 0 && (
                    <div className="col-span-full text-center py-20 anim-fade-in">
                        <span className="text-3xl mb-3 block">🔍</span>
                        <p className="text-text3" style={{ fontSize: 14 }}>No trends found</p>
                    </div>
                )}
            </section>
        </div>
    );
}
