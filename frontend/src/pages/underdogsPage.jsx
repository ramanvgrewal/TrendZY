import { useParams, useNavigate } from 'react-router-dom';
import { useInfiniteQuery } from '@tanstack/react-query';
import { getUnderdogs } from '../api/underdogs';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import UnderdogCard from '../components/ui/UnderdogCard';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';

const SECTIONS = [
    {
        key: 'ALL',
        label: 'All',
        emoji: '✦',
        description: 'All underdog D2C products from indie Instagram brands',
        color: '#a3e635',
    },
    {
        key: 'CRICKET',
        label: 'Cricket',
        emoji: '🏏',
        description: 'Match-day tees, fan jerseys, creator merch & cricket-inspired drops',
        color: '#fbbf24',
    },
    {
        key: 'GYM',
        label: 'Gym',
        emoji: '💪',
        description: 'Gym fits, oversized workout tees, lifting accessories & fitness creator merch',
        color: '#f97316',
    },
    {
        key: 'ANIME',
        label: 'Anime',
        emoji: '⚔️',
        description: 'Anime-inspired clothing, graphic tees & accessories from indie Indian brands',
        color: '#c084fc',
    },
    {
        key: 'STREETWEAR',
        label: 'Streetwear',
        emoji: '🛹',
        description: 'Underground drops, indie streetwear labels & limited-run pieces',
        color: '#60a5fa',
    },
    {
        key: 'SNEAKERS',
        label: 'Sneakers',
        emoji: '👟',
        description: 'Indie sneaker brands, limited collabs & sneaker-culture accessories',
        color: '#34d399',
    },
    {
        key: 'CODING',
        label: 'Coding',
        emoji: '💻',
        description: 'Developer culture merch, tech-themed fits & keyboard accessories',
        color: '#a3e635',
    },
];

export default function UnderdogsPage() {
    const { section: sectionParam } = useParams();
    const navigate = useNavigate();

    const activeKey = sectionParam ? sectionParam.toUpperCase() : 'ALL';
    const activeSection = SECTIONS.find((s) => s.key === activeKey) || SECTIONS[0];

    const { 
        data, 
        isLoading, 
        isError, 
        refetch,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage
    } = useInfiniteQuery({
        queryKey: ['underdogs', activeKey],
        queryFn: ({ pageParam = 0 }) => getUnderdogs(activeKey === 'ALL' ? null : activeKey, pageParam, 20),
        initialPageParam: 0,
        getNextPageParam: (lastPage, allPages) => lastPage.length === 20 ? allPages.length : undefined,
        staleTime: 3 * 60 * 1000,
    });

    const products = data?.pages.flat() || [];

    const handleSectionClick = (section) => {
        if (section.key === 'ALL') navigate('/underdogs');
        else navigate(`/underdogs/${section.key.toLowerCase()}`);
    };

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Navbar />

            <main style={{ flex: 1 }}>

                {/* ═══ PAGE HEADER ═══ */}
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '40px 16px 32px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
            <span className="font-mono" style={{
                fontSize: 10, fontWeight: 700, letterSpacing: '0.18em', textTransform: 'uppercase',
                padding: '4px 14px', borderRadius: 6,
                background: 'rgba(196,181,253,0.12)', color: '#c4b5fd',
                border: '1px solid rgba(196,181,253,0.25)',
            }}>
              ✦ D2C Discovery
            </span>
                    </div>
                    <h1 className="font-display" style={{
                        fontSize: 'clamp(40px, 7vw, 72px)', fontWeight: 900,
                        letterSpacing: '-0.02em', lineHeight: 1, margin: '0 0 10px', color: '#f5f5f5',
                    }}>
                        UNDERDOGS
                    </h1>
                    <p className="font-mono" style={{
                        fontSize: 12, color: 'rgba(255,255,255,0.4)', maxWidth: 480, lineHeight: 1.6,
                    }}>
                        Hand-scraped drops from indie Instagram brands — before they blow up on mainstream.
                    </p>
                </div>

                {/* ═══ SECTION TABS ═══ */}
                <div style={{
                    borderTop: '1px solid hsl(0 0% 14%)',
                    borderBottom: '1px solid hsl(0 0% 14%)',
                    background: 'hsl(0 0% 6%)',
                    position: 'sticky',
                    top: 64,
                    zIndex: 30,
                }}>
                    <div
                        style={{
                            maxWidth: 1280, margin: '0 auto', padding: '0 16px',
                            display: 'flex', gap: 4, overflowX: 'auto',
                        }}
                        className="scrollbar-hide"
                    >
                        {SECTIONS.map((section) => {
                            const active = section.key === activeKey;
                            return (
                                <button
                                    key={section.key}
                                    onClick={() => handleSectionClick(section)}
                                    className="font-mono"
                                    style={{
                                        flexShrink: 0,
                                        fontSize: 11, fontWeight: active ? 700 : 500,
                                        letterSpacing: '0.08em', textTransform: 'uppercase',
                                        padding: '14px 16px',
                                        borderBottom: active ? `2px solid ${section.color}` : '2px solid transparent',
                                        color: active ? section.color : 'rgba(255,255,255,0.4)',
                                        background: 'transparent',
                                        border: 'none',
                                        cursor: 'pointer',
                                        transition: 'all 0.2s',
                                        display: 'flex', alignItems: 'center', gap: 6,
                                    }}
                                    onMouseEnter={(e) => { if (!active) e.currentTarget.style.color = 'rgba(255,255,255,0.8)'; }}
                                    onMouseLeave={(e) => { if (!active) e.currentTarget.style.color = 'rgba(255,255,255,0.4)'; }}
                                >
                                    <span>{section.emoji}</span>
                                    <span>{section.label}</span>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* ═══ SECTION DESCRIPTION ═══ */}
                <div style={{
                    maxWidth: 1280, margin: '0 auto', padding: '24px 16px 20px',
                    display: 'flex', alignItems: 'center', gap: 12,
                }}>
                    <span style={{ fontSize: 28 }}>{activeSection.emoji}</span>
                    <div>
                        <h2 className="font-display" style={{ fontSize: 24, fontWeight: 800, color: '#f5f5f5', margin: 0 }}>
                            {activeSection.label}
                        </h2>
                        <p className="font-mono" style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', margin: '3px 0 0' }}>
                            {activeSection.description}
                        </p>
                    </div>
                    {!isLoading && products?.length > 0 && (
                        <span className="font-mono" style={{
                            marginLeft: 'auto', fontSize: 10, fontWeight: 700, letterSpacing: '0.1em',
                            textTransform: 'uppercase', padding: '4px 12px', borderRadius: 20,
                            background: `${activeSection.color}14`, color: activeSection.color,
                            border: `1px solid ${activeSection.color}28`, flexShrink: 0,
                        }}>
              {products.length} drops
            </span>
                    )}
                </div>

                {/* ═══ PRODUCT GRID ═══ */}
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 60px' }}>
                    {isLoading ? (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))', gap: 16 }}>
                            {[...Array(12)].map((_, i) => <SkeletonCard key={i} />)}
                        </div>
                    ) : isError ? (
                        <ErrorState message="Could not load underdog products" onRetry={refetch} />
                    ) : products && products.length > 0 ? (
                        <div>
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))',
                                gap: 16,
                            }}>
                                {products.map((product) => (
                                    <UnderdogCard key={product.id} product={product} />
                                ))}
                            </div>
                            
                            {hasNextPage && (
                                <div style={{ display: 'flex', justifyContent: 'center', marginTop: 40 }}>
                                    <button
                                        onClick={() => fetchNextPage()}
                                        disabled={isFetchingNextPage}
                                        className="font-mono"
                                        style={{
                                            padding: '12px 24px',
                                            background: '#f5f5f5',
                                            color: '#0a0a0a',
                                            border: 'none',
                                            borderRadius: 8,
                                            fontWeight: 600,
                                            fontSize: 12,
                                            letterSpacing: '0.05em',
                                            textTransform: 'uppercase',
                                            cursor: isFetchingNextPage ? 'not-allowed' : 'pointer',
                                            transition: 'opacity 0.2s'
                                        }}
                                    >
                                        {isFetchingNextPage ? 'Loading...' : 'Load More Drops'}
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <EmptyState
                            message={`No ${activeSection.label} drops yet`}
                            subMessage="The scraper is hunting. Check back soon — new drops added regularly."
                        />
                    )}
                </div>

            </main>

            <Footer />
        </div>
    );
}