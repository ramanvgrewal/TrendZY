import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import toast from 'react-hot-toast';
import useFilterStore from '../store/filterStore';
import { getHeroTrend, getTrendStats, getTrends } from '../api/trends';
import { getCurated } from '../api/curated';
import { getAffiliateLink, trackClick } from '../api/affiliate';
import { groupByProductType } from '../utils/productTypeClassifier';

import Navbar from '../components/layout/Navbar';
import TickerBar from '../components/layout/TickerBar';
import Footer from '../components/layout/Footer';
import VibeFilter from '../components/ui/VibeFilter';
import CategoryRow from '../components/ui/CategoryRow';
import ErrorState from '../components/ui/ErrorState';
import VelocityBadge from '../components/ui/VelocityBadge';
import ScoreRing from '../components/ui/ScoreRing';
import SkeletonCard from '../components/ui/SkeletonCard';

/* ── Section divider heading ── */
function SectionDivider({ badge, title, subtitle, badgeColor = '#a3e635', live }) {
  return (
      <div style={{ maxWidth: 1280, margin: '0 auto 20px', padding: '0 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
          {live && (
              <span style={{
                width: 7, height: 7, borderRadius: '50%',
                background: badgeColor, display: 'inline-block',
                animation: 'pulse-live 1.8s ease-in-out infinite',
              }} />
          )}
          <span className="font-mono" style={{
            fontSize: 10, fontWeight: 700, letterSpacing: '0.18em',
            textTransform: 'uppercase', padding: '3px 10px', borderRadius: 5,
            background: `${badgeColor}14`, color: badgeColor,
            border: `1px solid ${badgeColor}30`,
          }}>
          {badge}
        </span>
        </div>
        <h2 className="font-display" style={{
          fontSize: 'clamp(26px, 3.5vw, 38px)', fontWeight: 900,
          color: '#f5f5f5', letterSpacing: '-0.01em', lineHeight: 1.1, margin: 0,
        }}>
          {title}
        </h2>
        {subtitle && (
            <p className="font-mono" style={{ fontSize: 11, color: 'rgba(255,255,255,0.38)', marginTop: 4 }}>
              {subtitle}
            </p>
        )}
      </div>
  );
}

/* ── Skeleton placeholder ── */
function SkeletonSection() {
  return (
      <div style={{ marginBottom: 36 }}>
        <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 12px', display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ width: 3, height: 40, borderRadius: 3, background: 'hsl(0 0% 16%)' }} />
          <div style={{ width: 24, height: 24, borderRadius: 4, background: 'hsl(0 0% 16%)' }} />
          <div>
            <div style={{ width: 140, height: 16, borderRadius: 4, background: 'hsl(0 0% 16%)', marginBottom: 5 }} className="anim-shimmer" />
            <div style={{ width: 60, height: 9, borderRadius: 4, background: 'hsl(0 0% 16%)' }} className="anim-shimmer" />
          </div>
        </div>
        <div style={{ display: 'flex', gap: 12, padding: '0 16px', overflowX: 'hidden', maxWidth: 1280, margin: '0 auto' }}>
          {[...Array(5)].map((_, i) => (
              <div key={i} style={{ flexShrink: 0, width: 210 }}><SkeletonCard /></div>
          ))}
        </div>
      </div>
  );
}

/* ── Horizontal rule ── */
function Divider() {
  return (
      <div style={{ maxWidth: 1280, margin: '32px auto 36px', padding: '0 16px' }}>
        <div style={{ height: 1, background: 'linear-gradient(to right, transparent, hsl(0 0% 18%), transparent)' }} />
      </div>
  );
}

export default function HomePage() {
  const { activeVibe } = useFilterStore();
  const [heroBuyLoading, setHeroBuyLoading] = useState(false);
  const vibeParam = activeVibe === 'All' ? null : activeVibe;

  /* ── Queries ── */
  const { data: hero, isLoading: heroLoading, isError: heroError, refetch: refetchHero } =
      useQuery({ queryKey: ['hero'], queryFn: getHeroTrend });

  const { data: stats, isLoading: statsLoading, isError: statsError } =
      useQuery({ queryKey: ['stats'], queryFn: getTrendStats });

  const { data: trendingData, isLoading: trendingLoading, isError: trendingError, refetch: refetchTrending } =
      useQuery({ queryKey: ['trending', activeVibe], queryFn: () => getTrends('trending', vibeParam, 0, 100) });

  const { data: risingData, isLoading: risingLoading, isError: risingError, refetch: refetchRising } =
      useQuery({ queryKey: ['rising', activeVibe], queryFn: () => getTrends('rising', vibeParam, 0, 100) });

  const { data: curatedData, isLoading: curatedLoading, isError: curatedError, refetch: refetchCurated } =
      useQuery({ queryKey: ['curated-home', activeVibe], queryFn: () => getCurated(vibeParam, null, 0, 100) });

  /* ── Smart type-grouping ── */
  const trendingGroups = useMemo(() => groupByProductType(trendingData), [trendingData]);
  const risingGroups   = useMemo(() => groupByProductType(risingData),   [risingData]);
  const curatedGroups  = useMemo(() => groupByProductType(curatedData),  [curatedData]);

  /* ── Hero buy ── */
  const handleHeroBuy = async () => {
    if (!hero) return;
    setHeroBuyLoading(true);
    try {
      const { affiliateUrl } = await getAffiliateLink(hero.id, hero.platform);
      window.open(affiliateUrl, '_blank', 'noopener,noreferrer');
      trackClick(hero.id, hero.platform, 'hero_banner');
    } catch { toast.error('Could not get product link. Try again.'); }
    finally { setHeroBuyLoading(false); }
  };

  return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
        <TickerBar />
        <Navbar />

        <main style={{ flex: 1 }}>

          {/* ════ HERO ════ */}
          <div style={{ maxWidth: 1280, margin: '0 auto', padding: '24px 16px 0' }}>
            {heroLoading ? (
                <div className="anim-shimmer" style={{ width: '100%', height: 420, borderRadius: 18 }} />
            ) : heroError ? (
                <ErrorState message="Could not load featured trend" onRetry={refetchHero} />
            ) : hero ? (
                <div
                    style={{
                      position: 'relative', width: '100%', borderRadius: 18,
                      overflow: 'hidden', border: '1px solid hsl(0 0% 16%)', minHeight: 400,
                    }}
                    className="group"
                >
                  {hero.imageUrl ? (
                      <>
                        <img
                            src={hero.imageUrl}
                            alt={hero.productName}
                            style={{
                              position: 'absolute', inset: 0,
                              width: '100%', height: '100%',
                              objectFit: 'cover', objectPosition: 'top center',
                              transition: 'transform 700ms ease',
                            }}
                            className="group-hover:scale-105"
                            onError={(e) => (e.currentTarget.style.display = 'none')}
                        />
                        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to right, rgba(0,0,0,0.92) 0%, rgba(0,0,0,0.55) 55%, rgba(0,0,0,0.1) 100%)' }} />
                        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 50%)' }} />
                      </>
                  ) : (
                      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg, #121212 0%, #1a1a1a 100%)' }} />
                  )}

                  <div style={{
                    position: 'relative', padding: 'clamp(28px, 5vw, 52px)',
                    display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', minHeight: 400,
                  }}>
                    <div style={{ maxWidth: 520 }}>
                      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 10, marginBottom: 14 }}>
                    <span className="font-mono" style={{
                      fontSize: 10, fontWeight: 700, letterSpacing: '0.16em',
                      textTransform: 'uppercase', padding: '4px 12px', borderRadius: 6,
                      background: '#a3e635', color: '#000',
                    }}>
                      #1 Trending
                    </span>
                        {hero.velocityLabel && <VelocityBadge label={hero.velocityLabel} />}
                      </div>

                      <h1 className="font-display" style={{
                        fontSize: 'clamp(40px, 6vw, 72px)', fontWeight: 900,
                        color: '#f5f5f5', lineHeight: 0.95, letterSpacing: '-0.01em', marginBottom: 12,
                      }}>
                        {hero.productName}
                      </h1>

                      <div className="font-mono" style={{
                        display: 'flex', alignItems: 'center', gap: 10,
                        fontSize: 12, color: '#a3e635',
                        textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 18,
                      }}>
                        {hero.category && <span>{hero.category}</span>}
                        {hero.category && hero.estimatedPrice > 0 && <span style={{ color: 'rgba(255,255,255,0.25)' }}>·</span>}
                        {hero.estimatedPrice > 0 && <span style={{ color: '#f5f5f5' }}>Est. ₹{hero.estimatedPrice.toLocaleString('en-IN')}</span>}
                      </div>

                      {hero.totalSignals > 0 && (
                          <p className="font-mono" style={{
                            fontSize: 11, color: 'rgba(255,255,255,0.4)',
                            borderLeft: '2px solid rgba(163,230,53,0.4)', paddingLeft: 10, marginBottom: 24,
                          }}>
                            Detected across {hero.detectedSubreddits?.length || 0} subreddits · {hero.totalSignals} mentions
                          </p>
                      )}

                      <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
                        <button
                            onClick={handleHeroBuy}
                            disabled={heroBuyLoading}
                            className="font-mono"
                            style={{
                              fontSize: 11, fontWeight: 700, letterSpacing: '0.1em',
                              textTransform: 'uppercase', padding: '13px 28px', borderRadius: 12,
                              background: '#f5f5f5', color: '#000',
                              border: 'none', cursor: heroBuyLoading ? 'wait' : 'pointer',
                              transition: 'background 0.2s',
                              display: 'flex', alignItems: 'center', gap: 8,
                            }}
                            onMouseEnter={(e) => { e.currentTarget.style.background = '#a3e635'; }}
                            onMouseLeave={(e) => { e.currentTarget.style.background = '#f5f5f5'; }}
                        >
                          {heroBuyLoading
                              ? <div style={{ width: 16, height: 16, borderRadius: '50%', border: '2px solid rgba(0,0,0,0.2)', borderTopColor: '#000', animation: 'spin 0.7s linear infinite' }} />
                              : `SHOP ON ${(hero.platform || 'STORE').toUpperCase()} →`
                          }
                        </button>
                        {hero.trendScore > 0 && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <ScoreRing score={hero.trendScore} size={44} strokeWidth={4} />
                              <span className="font-mono" style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', textTransform: 'uppercase', letterSpacing: '0.1em', lineHeight: 1.4 }}>
                          Trend<br />Score
                        </span>
                            </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
            ) : null}
          </div>

          {/* ════ VIBE FILTER ════ */}
          <VibeFilter />

          {/* ════ LIVE STATS ════ */}
          {!statsError && (
              <div style={{ borderBottom: '1px solid hsl(0 0% 14%)', background: 'hsl(0 0% 6%)', marginBottom: 12 }}>
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '10px 16px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 20 }} className="font-mono">
                  {statsLoading ? (
                      <div style={{ height: 12, width: 200, borderRadius: 4, background: 'hsl(0 0% 14%)' }} className="anim-shimmer" />
                  ) : stats ? (
                      <>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
                          <span className="live-dot" />
                          <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.14em', color: '#a3e635', textTransform: 'uppercase' }}>Live Engine</span>
                        </div>
                        {[['Signals', stats.signalsToday], ['Trending', stats.trendingCount], ['Rising', stats.risingCount], ['Subreddits', stats.subredditsMonitored]].map(([label, value]) =>
                                value ? (
                                    <span key={label} style={{ fontSize: 10, letterSpacing: '0.06em', color: 'rgba(255,255,255,0.35)', textTransform: 'uppercase' }}>
                        {label}: <span style={{ color: 'rgba(255,255,255,0.75)' }}>{value}</span>
                      </span>
                                ) : null
                        )}
                      </>
                  ) : null}
                </div>
              </div>
          )}

          {/* ════ TRENDING — by product type ════ */}
          <div style={{ paddingTop: 32, paddingBottom: 4 }}>
            <SectionDivider
                badge="Trending Now"
                title="Blowing Up Right Now"
                subtitle="Highest velocity signals — browse by what you're actually looking for."
                badgeColor="#a3e635"
                live
            />
            {trendingLoading ? (
                <><SkeletonSection /><SkeletonSection /><SkeletonSection /></>
            ) : trendingError ? (
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px' }}>
                  <ErrorState message="Could not load trending products" onRetry={refetchTrending} />
                </div>
            ) : trendingGroups.length > 0 ? (
                trendingGroups.map(({ cfg, products }) => (
                    <CategoryRow key={`t-${cfg.key}`} cfg={cfg} products={products} source="home_trending" />
                ))
            ) : (
                <p className="font-mono" style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 24px', fontSize: 13, color: 'rgba(255,255,255,0.3)' }}>
                  No trending products for this vibe.
                </p>
            )}
          </div>

          <Divider />

          {/* ════ RISING — by product type ════ */}
          <div style={{ paddingBottom: 4 }}>
            <SectionDivider
                badge="Rising Fast"
                title="Catch the Wave"
                subtitle="Emerging signals — get in before everyone else does."
                badgeColor="#fbbf24"
                live
            />
            {risingLoading ? (
                <><SkeletonSection /><SkeletonSection /></>
            ) : risingError ? (
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px' }}>
                  <ErrorState message="Could not load rising products" onRetry={refetchRising} />
                </div>
            ) : risingGroups.length > 0 ? (
                risingGroups.map(({ cfg, products }) => (
                    <CategoryRow key={`r-${cfg.key}`} cfg={cfg} products={products} source="home_rising" />
                ))
            ) : (
                <p className="font-mono" style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 24px', fontSize: 13, color: 'rgba(255,255,255,0.3)' }}>
                  No rising products for this vibe.
                </p>
            )}
          </div>

          {/* ════ ONLY ON TRENDZY — by product type ════ */}
          {(curatedLoading || curatedGroups.length > 0 || curatedError) && (
              <>
                <Divider />
                <div style={{ paddingBottom: 40 }}>
                  <SectionDivider
                      badge="Only on TrendZY"
                      title="Hand-Picked Drops"
                      subtitle="Indie brands & hidden gems you won't find on Amazon or Myntra."
                      badgeColor="#c4b5fd"
                  />
                  {curatedLoading ? (
                      <SkeletonSection />
                  ) : curatedError ? (
                      <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px' }}>
                        <ErrorState message="Could not load curated products" onRetry={refetchCurated} />
                      </div>
                  ) : curatedGroups.length > 0 ? (
                      curatedGroups.map(({ cfg, products }) => (
                          <CategoryRow key={`c-${cfg.key}`} cfg={cfg} products={products} source="home_curated" />
                      ))
                  ) : null}
                </div>
              </>
          )}
        </main>

        <Footer />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
  );
}