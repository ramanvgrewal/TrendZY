import { useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import StatsBar from '../components/StatsBar';
import TrendCard from '../components/TrendCard';
import Section from '../components/Section';
import VibeTagFilter from '../components/VibeTagFilter';
import { fetchRisingTrends, fetchTrends, fetchSections, fetchVibes } from '../api/client';
import { getSessionId } from '../utils/session';

export default function Index({ mode = 'discover' }) {
  const { searchTerm = '' } = useOutletContext() || {};
  const [trends, setTrends] = useState([]);
  const [sections, setSections] = useState([]);
  const [risingTrends, setRisingTrends] = useState([]);
  const [vibes, setVibes] = useState([]);
  const [activeVibe, setActiveVibe] = useState('');

  useEffect(() => {
    const initLoad = async () => {
      const [vibesData, risingData, sectionsData] = await Promise.all([
        fetchVibes(), fetchRisingTrends(), fetchSections()
      ]);
      setVibes(vibesData);
      setRisingTrends(risingData);
      setSections(sectionsData);
    };
    initLoad();
  }, []);

  useEffect(() => {
    const load = async () => {
      const params = { sessionId: getSessionId(), indiaRelevant: true };
      if (activeVibe) params.vibe = activeVibe;
      const data = await fetchTrends(params);
      setTrends(data);
    };
    load();
  }, [activeVibe, mode]);

  const visibleTrends = useMemo(() => {
    const term = searchTerm.toLowerCase().trim();
    if (!term) return trends;
    return trends.filter((trend) => {
      return (
        trend.productName?.toLowerCase().includes(term) ||
        trend.category?.toLowerCase().includes(term) ||
        trend.brandMention?.toLowerCase().includes(term) ||
        trend.subcategory?.toLowerCase().includes(term)
      );
    });
  }, [searchTerm, trends]);

  // If filtering by vibe or searching, show grid
  const isFiltering = activeVibe !== '' || (searchTerm && searchTerm.trim() !== '');

  return (
    <div className="min-h-screen bg-background text-foreground">
      <section className="relative overflow-hidden border-b border-border">
        <div className="pointer-events-none absolute left-1/2 top-0 h-80 w-[52rem] -translate-x-1/2 rounded-full bg-[radial-gradient(circle,hsl(var(--primary)/0.08),transparent_60%)]" />
        <div className="mx-auto max-w-7xl px-4 pb-16 pt-14 sm:px-6 lg:px-8">
          <p className="mb-3 font-body text-xs uppercase tracking-[0.2em] text-muted">Trend Discovery Dashboard</p>
          <h1 className="max-w-5xl font-display text-[clamp(5rem,11vw,7rem)] font-bold italic leading-[0.95] text-glow animate-hero-slide-up">
            <span className="gradient-text">before they explode</span>
          </h1>
          <p className="mt-4 max-w-2xl font-body text-base text-muted opacity-0 animate-hero-fade [animation-delay:120ms] [animation-fill-mode:forwards]">
            Surface breakout products from Reddit and YouTube before they hit mainstream demand.
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-7xl space-y-10 px-4 py-8 sm:px-6 lg:px-8">
        <StatsBar />

        <section>
          <h2 className="font-display text-2xl font-semibold text-foreground mb-4">India Trends</h2>
          <VibeTagFilter vibes={vibes} activeVibe={activeVibe} onVibeChange={setActiveVibe} />
        </section>

        {!isFiltering ? (
          <div className="space-y-12">
            {risingTrends && risingTrends.length > 0 && (
              <Section title="Blowing Up Right Now" emoji="🔥" trends={risingTrends} type="featured" />
            )}
            {sections && sections.map(sec => (
              <Section key={sec.subcategory} title={sec.subcategory} emoji={sec.emoji} trends={sec.trends} />
            ))}
          </div>
        ) : (
          <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {visibleTrends.map((trend) => (
              <TrendCard key={trend.id} trend={trend} />
            ))}
          </section>
        )}
      </main>
    </div>
  );
}
