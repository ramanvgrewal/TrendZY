import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useState } from 'react';
import toast from 'react-hot-toast';
import useFilterStore from '../store/filterStore';
import { getHeroTrend, getTrendStats, getTrends } from '../api/trends';
import { getCurated } from '../api/curated';
import { getAffiliateLink, trackClick } from '../api/affiliate';

// Components
import Navbar from '../components/layout/Navbar';
import TickerBar from '../components/layout/TickerBar';
import Footer from '../components/layout/Footer';
import VibeFilter from '../components/ui/VibeFilter';
import ScrollRow from '../components/ui/ScrollRow';
import ErrorState from '../components/ui/ErrorState';
import VelocityBadge from '../components/ui/VelocityBadge';
import ScoreRing from '../components/ui/ScoreRing';

export default function HomePage() {
  const [searchParams] = useSearchParams();
  const { activeVibe } = useFilterStore();
  
  // Local state for Hero button
  const [heroBuyLoading, setHeroBuyLoading] = useState(false);

  // Queries
  const { 
    data: hero, 
    isLoading: heroLoading, 
    isError: heroError, 
    refetch: refetchHero 
  } = useQuery({ queryKey: ['hero'], queryFn: getHeroTrend });

  const { 
    data: stats, 
    isLoading: statsLoading, 
    isError: statsError 
  } = useQuery({ queryKey: ['stats'], queryFn: getTrendStats });

  const vibeParam = activeVibe === 'All' ? null : activeVibe;

  const { 
    data: trendingData, 
    isLoading: trendingLoading, 
    isError: trendingError, 
    refetch: refetchTrending 
  } = useQuery({ 
    queryKey: ['trending', activeVibe], 
    queryFn: () => getTrends('trending', vibeParam) 
  });

  const { 
    data: risingData, 
    isLoading: risingLoading, 
    isError: risingError, 
    refetch: refetchRising 
  } = useQuery({ 
    queryKey: ['rising', activeVibe], 
    queryFn: () => getTrends('rising', vibeParam) 
  });

  const { 
    data: curatedData, 
    isLoading: curatedLoading, 
    isError: curatedError, 
    refetch: refetchCurated 
  } = useQuery({ 
    queryKey: ['curated-home', activeVibe], 
    queryFn: () => getCurated(vibeParam, null) 
  });

  const handleHeroBuy = async () => {
    if (!hero) return;
    setHeroBuyLoading(true);
    try {
      const { affiliateUrl } = await getAffiliateLink(hero.id, hero.platform);
      window.open(affiliateUrl, '_blank', 'noopener,noreferrer');
      trackClick(hero.id, hero.platform, 'hero_banner');
    } catch (err) {
      toast.error('Could not get product link. Try again.');
    } finally {
      setHeroBuyLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      <TickerBar />
      <Navbar />

      <main className="flex-1">
        {/* HERO SECTION */}
        {heroLoading ? (
          <div className="w-full max-w-7xl mx-auto px-4 mt-8">
            <div className="w-full h-[400px] bg-[#1a1a1a] animate-pulse rounded-2xl"></div>
          </div>
        ) : heroError ? (
          <div className="w-full max-w-7xl mx-auto px-4 mt-8">
            <ErrorState message="Could not load featured trend" onRetry={refetchHero} />
          </div>
        ) : hero ? (
          <div className="w-full max-w-7xl mx-auto px-4 mt-8 mb-8">
            <div className="relative w-full h-[400px] rounded-2xl overflow-hidden border border-border group">
              {/* Background */}
              {hero.imageUrl ? (
                <div className="absolute inset-0">
                  <img
                    src={hero.imageUrl}
                    alt={hero.productName}
                    className="w-full h-full object-cover object-top transition-transform duration-700 group-hover:scale-105"
                    onError={(e) => { e.currentTarget.style.display = 'none'; }}
                  />
                  <div className="absolute inset-0 bg-gradient-to-r from-[#080808] via-[#080808]/70 to-transparent" />
                </div>
              ) : (
                <div className="absolute inset-0 bg-gradient-to-br from-[#121212] to-[#1a1a1a]"></div>
              )}
              
              {/* Overlay Gradient */}
              <div className="absolute inset-0 bg-gradient-to-t from-bg via-bg/80 to-transparent"></div>
              <div className="absolute inset-0 bg-gradient-to-r from-bg/90 via-bg/40 to-transparent"></div>

              {/* Content Box */}
              <div className="absolute inset-0 p-8 md:p-12 flex flex-col justify-end">
                <div className="max-w-xl">
                  <div className="flex flex-wrap items-center gap-3 mb-4">
                    <span className="bg-lime-400 text-black font-mono font-bold text-xs uppercase tracking-widest px-3 py-1 rounded">
                      #1 Trending
                    </span>
                    {hero.velocityLabel && <VelocityBadge label={hero.velocityLabel} />}
                  </div>

                  <h1 className="font-display font-black text-5xl md:text-6xl text-white tracking-tight mb-2 leading-none">
                    {hero.productName}
                  </h1>
                  
                  <div className="flex items-center gap-3 font-mono text-sm text-lime-400 uppercase tracking-wider mb-6">
                    {hero.category && <span>{hero.category}</span>}
                    {hero.estimatedPrice > 0 && (
                      <>
                        <span className="text-white/30">•</span>
                        <span className="text-white">Est. ₹{hero.estimatedPrice}</span>
                      </>
                    )}
                  </div>

                  {hero.totalSignals > 0 && (
                    <p className="font-mono text-xs text-white/50 mb-8 border-l-2 border-lime-400/50 pl-3">
                      Detected across {hero.detectedSubreddits?.length || 0} subreddits · {hero.totalSignals} mentions
                    </p>
                  )}

                  <div className="flex items-center gap-6">
                    <button 
                      onClick={handleHeroBuy}
                      disabled={heroBuyLoading}
                      className="bg-white hover:bg-lime-400 text-black font-mono font-bold tracking-widest uppercase px-8 py-3.5 rounded-xl transition-all flex items-center gap-2"
                    >
                      {heroBuyLoading ? (
                        <div className="w-5 h-5 border-2 border-black/20 border-t-black rounded-full animate-spin"></div>
                      ) : (
                        `SHOP ON ${(hero.platform || 'STORE').toUpperCase()}`
                      )}
                    </button>
                    {hero.trendScore > 0 && (
                      <div className="hidden sm:flex items-center gap-3">
                        <ScoreRing score={hero.trendScore} size={48} strokeWidth={4} />
                        <span className="font-mono text-xs text-white/50 uppercase tracking-wider w-16 leading-tight">
                          Trend<br/>Score
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : null}

        {/* VIBE FILTER */}
        <VibeFilter />

        {/* LIVE STATS BAR */}
        {!statsError && (
          <div className="border-b border-border bg-surface/30">
            <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-4 font-mono text-xs text-white/50 uppercase tracking-widest">
              {statsLoading ? (
                <div className="w-full flex justify-between animate-pulse">
                  <div className="h-4 w-32 bg-[#1a1a1a] rounded"></div>
                  <div className="h-4 w-32 bg-[#1a1a1a] rounded hidden sm:block"></div>
                  <div className="h-4 w-32 bg-[#1a1a1a] rounded hidden md:block"></div>
                  <div className="h-4 w-32 bg-[#1a1a1a] rounded hidden lg:block"></div>
                </div>
              ) : stats ? (
                <>
                  <div className="flex items-center gap-2">
                    <div className="live-dot"></div>
                    <span className="text-lime-400">Live Engine</span>
                  </div>
                  <div className="hidden sm:block">Signals Today: <span className="text-white">{stats.signalsToday || '—'}</span></div>
                  <div>Trending: <span className="text-white">{stats.trendingCount || '—'}</span></div>
                  <div className="hidden md:block">Rising: <span className="text-white">{stats.risingCount || '—'}</span></div>
                  <div className="hidden lg:block">Subreddits: <span className="text-white">{stats.subredditsMonitored || '—'}</span></div>
                  <div className="hidden xl:block">Updated: <span className="text-white">{stats.lastCollectionTime ? new Date(stats.lastCollectionTime).toLocaleTimeString() : '—'}</span></div>
                </>
              ) : null}
            </div>
          </div>
        )}

        {/* TRENDING SECTION */}
        <ScrollRow 
          title="Blowing Up Right Now" 
          subtitle="The highest velocity trends across the Indian internet."
          badge="Trending"
          badgeColor="lime"
          isLoading={trendingLoading}
          isError={trendingError}
          data={trendingData}
          onRetry={refetchTrending}
          featured={true}
          source="home_trending"
        />

        {/* RISING SECTION */}
        <ScrollRow 
          title="Catch The Wave" 
          subtitle="Emerging signals. Get in before everyone else does."
          badge="Rising"
          badgeColor="amber"
          isLoading={risingLoading}
          isError={risingError}
          data={risingData}
          onRetry={refetchRising}
          source="home_rising"
        />

        {/* CURATED SECTION */}
        {((curatedLoading) || (curatedData?.content?.length > 0) || (Array.isArray(curatedData) && curatedData.length > 0) || curatedError) && (
          <div className="mt-24">
            <div className="max-w-7xl mx-auto px-4 mb-4">
              <div className="h-px w-full bg-gradient-to-r from-transparent via-border to-transparent"></div>
            </div>
            <ScrollRow 
              title="Only On TrendZY" 
              subtitle="Hand-picked drops from indie darlings and homegrown labels."
              badge="Curated"
              badgeColor="purple"
              isLoading={curatedLoading}
              isError={curatedError}
              data={curatedData}
              onRetry={refetchCurated}
              source="home_curated"
            />
          </div>
        )}
      </main>
      
      <Footer />
    </div>
  );
}
