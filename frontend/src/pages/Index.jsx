import { useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import CategoryFilter from '../components/CategoryFilter';
import StatsBar from '../components/StatsBar';
import TrendCard from '../components/TrendCard';
import { fetchRisingTrends, fetchTrends } from '../api/client';
import { getSessionId } from '../utils/session';

const categories = ['All', 'Beauty', 'Fashion', 'Fitness', 'Home', 'Lifestyle', 'Other', 'Tech'];

function mapCategoryToApi(category) {
  if (category === 'All') return '';
  if (category === 'Other') return '';
  return category.toLowerCase();
}

export default function Index({ mode = 'discover' }) {
  const { searchTerm = '' } = useOutletContext() || {};
  const [activeCategory, setActiveCategory] = useState('All');
  const [trends, setTrends] = useState([]);

  useEffect(() => {
    const load = async () => {
      const params = { sessionId: getSessionId(), indiaRelevant: true };
      const category = mapCategoryToApi(activeCategory);
      if (category) params.category = category;

      const data = mode === 'rising' ? await fetchRisingTrends() : await fetchTrends(params);
      setTrends(data);
    };

    load();
  }, [activeCategory, mode]);

  const visibleTrends = useMemo(() => {
    const term = searchTerm.toLowerCase().trim();
    if (!term) return trends;
    return trends.filter((trend) => {
      return (
        trend.productName?.toLowerCase().includes(term) ||
        trend.category?.toLowerCase().includes(term) ||
        trend.brandMention?.toLowerCase().includes(term)
      );
    });
  }, [searchTerm, trends]);

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
            Surface breakout products from Reddit and social platforms before they hit mainstream demand.
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-7xl space-y-10 px-4 py-8 sm:px-6 lg:px-8">
        <StatsBar />

        <section>
          <h2 className="font-display text-2xl font-semibold text-foreground">India Trends</h2>
          <p className="mt-1 font-body text-sm text-muted">Early signals filtered for Indian market relevance.</p>
          <div className="mt-4">
            <CategoryFilter
              categories={categories}
              active={activeCategory}
              onChange={setActiveCategory}
            />
          </div>
        </section>

        <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {visibleTrends.map((trend) => (
            <TrendCard key={trend.id} trend={trend} />
          ))}
        </section>
      </main>
    </div>
  );
}
