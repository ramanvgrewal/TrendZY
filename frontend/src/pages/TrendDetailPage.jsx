import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchTrendById, fetchTrends, recordView, recordBuyClick, isBackendOffline } from '../api/client';
import { getSessionId } from '../utils/session';

const velMap = {
    'Rising Fast': { cls: 'vel-rising', icon: '🔥' },
    'Trending Now': { cls: 'vel-trending', icon: '📈' },
    'Underrated Gem': { cls: 'vel-underrated', icon: '💎' },
    'Emerging': { cls: 'vel-emerging', icon: '🌱' },
};

function scoreClass(s) {
    if (s >= 8) return 'score-red';
    if (s >= 6) return 'score-amber';
    if (s >= 4) return 'score-blue';
    return 'score-lime';
}

function getGrowth(tw, lw) {
    if (!lw || lw === 0) return 'New this week';
    return `+${Math.round(((tw - lw) / lw) * 100)}%`;
}

export default function TrendDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [trend, setTrend] = useState(null);
    const [related, setRelated] = useState([]);
    const [loading, setLoading] = useState(true);
    const [offline, setOffline] = useState(false);

    useEffect(() => {
        (async () => {
            setLoading(true);
            const data = await fetchTrendById(id);
            setOffline(isBackendOffline());
            if (!data) { navigate('/'); return; }
            setTrend(data);
            recordView(getSessionId(), data.id);
            if (data.category) {
                const all = await fetchTrends({ category: data.category });
                setRelated(all.filter(t => t.id !== data.id).slice(0, 3));
            }
            setLoading(false);
        })();
    }, [id]);

    const handleBuy = (link) => {
        recordBuyClick(getSessionId(), trend.id);
        window.open(link.affiliateUrl, '_blank');
    };

    if (loading) {
        return (
            <div className="max-w-4xl mx-auto px-4 py-16 anim-fade-in">
                <div className="anim-shimmer h-8 w-48 mb-8" />
                <div className="anim-shimmer h-24 w-36 mx-auto rounded mb-8" />
                <div className="space-y-3">
                    <div className="anim-shimmer h-5 w-full" />
                    <div className="anim-shimmer h-5 w-3/4" />
                    <div className="anim-shimmer h-5 w-1/2" />
                </div>
            </div>
        );
    }

    if (!trend) return null;

    const score = trend.trendScore || 0;
    const vel = velMap[trend.velocityLabel] || velMap['Emerging'];
    const growth = getGrowth(trend.mentionCountThisWeek, trend.mentionCountLastWeek);
    const hasLinks = trend.productLinks?.length > 0;

    const statsRow = [
        { label: 'This Week', value: trend.mentionCountThisWeek?.toLocaleString() ?? '0' },
        { label: 'Last Week', value: trend.mentionCountLastWeek?.toLocaleString() ?? '0' },
        { label: 'Growth', value: growth, accent: true },
        { label: 'Confidence', value: trend.confidence ? `${(trend.confidence * 100).toFixed(0)}%` : '—' },
    ];

    const tags = [
        trend.category && { icon: '📁', text: trend.category },
        trend.brandMention && { icon: '🏷️', text: trend.brandMention },
        trend.gender && { icon: '👤', text: trend.gender },
        trend.pricePoint && { icon: '💰', text: trend.pricePoint },
        trend.audienceTag && { icon: '🎯', text: trend.audienceTag },
    ].filter(Boolean);

    return (
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {offline && (
                <div className="mb-6 px-4 py-3 rounded-lg text-center"
                    style={{ fontSize: 12, fontWeight: 600, background: 'var(--color-amber-dim)', color: 'var(--color-amber)', border: '1px solid rgba(255,171,0,0.15)' }}>
                    ⚠️ Backend offline — showing cached data
                </div>
            )}

            {/* Back */}
            <button onClick={() => navigate(-1)}
                className="flex items-center gap-2 text-text3 hover:text-text transition-colors duration-200 mb-10 cursor-pointer group">
                <svg className="w-4 h-4 transition-transform duration-200 group-hover:-translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
                <span style={{ fontSize: 12, fontWeight: 500 }}>Back</span>
            </button>

            {/* Header: big Playfair score + name */}
            <div className="text-center mb-12 anim-fade-up">
                {/* Large score number */}
                <div className={`${scoreClass(score)} mb-3`}
                    style={{ fontFamily: 'var(--font-heading)', fontSize: 'clamp(72px, 12vw, 120px)', fontWeight: 900, lineHeight: 1 }}>
                    {score.toFixed(1)}
                </div>
                <span className={`vel-badge ${vel.cls} mb-4 inline-flex`}>{vel.icon} {trend.velocityLabel || 'Emerging'}</span>
                <h1 style={{ fontFamily: 'var(--font-heading)', fontSize: 'clamp(24px, 4vw, 40px)', fontWeight: 900, lineHeight: 1.2 }}
                    className="text-text mt-4">
                    {trend.productName}
                </h1>
            </div>

            {/* ── Product Image (if available) ── */}
            {trend.imageUrl && (
                <section className="mb-10 anim-fade-up s2">
                    <div className="trend-detail-image">
                        <img
                            src={trend.imageUrl}
                            alt={trend.productName}
                            onError={(e) => { e.target.parentElement.style.display = 'none'; }}
                        />
                        {trend.imageSource && (
                            <span className="trend-detail-img-source">
                                Image from {trend.imageSource === 'amazon' ? 'Amazon' : trend.imageSource === 'myntra' ? 'Myntra' : trend.imageSource}
                            </span>
                        )}
                    </div>
                </section>
            )}

            {/* AI Analysis */}
            <section className="glass-card p-6 mb-8 anim-fade-up s3">
                <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                    className="text-lime mb-3">AI Analysis</p>
                {trend.aiExplanation ? (
                    <p style={{ fontSize: 14, lineHeight: 1.8, fontWeight: 300 }} className="text-text2">
                        {trend.aiExplanation}
                    </p>
                ) : (
                    <p className="text-text3 italic" style={{ fontSize: 13 }}>Analysis pending…</p>
                )}
            </section>

            {/* Stats — horizontal row */}
            <section className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8 anim-fade-up s4">
                {statsRow.map(st => (
                    <div key={st.label} className="glass-card p-4 text-center">
                        <p style={{ fontSize: 10, fontWeight: 600, letterSpacing: 1.5, textTransform: 'uppercase' }}
                            className="text-text3 mb-2">{st.label}</p>
                        <p style={{ fontFamily: 'var(--font-heading)', fontSize: 22, fontWeight: 900 }}
                            className={st.accent ? 'text-lime' : 'text-text'}>
                            {st.value}
                        </p>
                    </div>
                ))}
            </section>

            {/* Tags */}
            {(tags.length > 0 || trend.indiaRelevant) && (
                <section className="flex flex-wrap gap-2 mb-8 anim-fade-up s5">
                    {tags.map(tag => (
                        <span key={tag.text}
                            style={{ fontSize: 11, padding: '5px 12px', borderRadius: 8, background: 'var(--color-card2)', border: '1px solid var(--color-border)' }}
                            className="text-text2 capitalize">{tag.icon} {tag.text}</span>
                    ))}
                    {trend.indiaRelevant && (
                        <span style={{ fontSize: 11, padding: '5px 12px', borderRadius: 8, background: 'var(--color-amber-dim)', border: '1px solid rgba(255,171,0,0.1)' }}
                            className="text-amber font-medium">🇮🇳 India Relevant</span>
                    )}
                </section>
            )}

            {/* Buy links */}
            <section className="mb-12 anim-fade-up s6">
                <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                    className="text-text3 mb-4">Shop Now</p>
                {hasLinks ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                        {trend.productLinks.map((link, i) => {
                            const buyUrl = (i === 0 && trend.directProductUrl) ? trend.directProductUrl : link.affiliateUrl;
                            return (
                                <button key={link.id || link.platform}
                                    onClick={() => { recordBuyClick(getSessionId(), trend.id); window.open(buyUrl, '_blank'); }}
                                    className={`py-3 px-4 rounded-lg font-semibold cursor-pointer transition-all duration-200 ${i === 0 ? 'btn-primary' : 'btn-secondary'}`}
                                    style={{ fontSize: 12 }}>
                                    {link.platform === 'Amazon' ? 'Buy on Amazon' : `Shop ${link.platform}`}
                                </button>
                            );
                        })}
                    </div>
                ) : (
                    <div className="glass-card text-center py-6 text-text3" style={{ fontSize: 12 }}>No buy links yet</div>
                )}
            </section>

            {/* Related */}
            {related.length > 0 && (
                <section className="anim-fade-up s6">
                    <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}
                        className="text-text3 mb-4">Related Trends</p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                        {related.map(r => (
                            <Link key={r.id} to={`/trend/${r.id}`}
                                className="glass-card p-4 block hover:border-lime-border transition-colors duration-200 group">
                                <span style={{ fontFamily: 'var(--font-heading)', fontSize: 14, fontWeight: 700 }}
                                    className="text-text group-hover:text-lime transition-colors duration-200">
                                    {r.productName}
                                </span>
                                <div className="flex items-center gap-2 mt-2">
                                    <span className="text-lime" style={{ fontSize: 12, fontWeight: 700, fontFamily: 'var(--font-heading)' }}>
                                        {r.trendScore?.toFixed(1)}
                                    </span>
                                    <span className="text-text3" style={{ fontSize: 11 }}>· {r.velocityLabel || 'Emerging'}</span>
                                </div>
                            </Link>
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
}
