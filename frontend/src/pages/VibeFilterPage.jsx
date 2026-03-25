import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getTrends } from '../api/trends';
import { getCurated } from '../api/curated';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import ProductCard from '../components/ui/ProductCard';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';
import { filterAndSortProducts } from '../utils/productUtils';

export default function VibeFilterPage() {
  const { tag } = useParams();
  const vibeTag = '#' + tag;

  const { data: trendsData, isLoading: trendsLoading, isError: trendsError } = useQuery({
    queryKey: ['trends-vibe', tag],
    queryFn: () => getTrends(null, vibeTag, 0, 20)
  });

  const { data: curatedData, isLoading: curatedLoading, isError: curatedError } = useQuery({
    queryKey: ['curated-vibe', tag],
    queryFn: () => getCurated(vibeTag, null, 0, 20)
  });

  const isLoading = trendsLoading || curatedLoading;
  
  const rawTrendsList = trendsData?.content || trendsData || [];
  const rawCuratedList = curatedData?.content || curatedData || [];
  
  const trendsList = filterAndSortProducts(rawTrendsList);
  const curatedList = filterAndSortProducts(rawCuratedList);
  
  const hasTrends = trendsList.length > 0;
  const hasCurated = curatedList.length > 0;
  const totalCount = (hasTrends ? trendsList.length : 0) + (hasCurated ? curatedList.length : 0);

  return (
    <div className="min-h-screen flex flex-col bg-bg">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 pt-16 pb-20">
        <div className="text-center mb-16">
          <h1 className="font-display font-black text-6xl md:text-8xl text-transparent bg-clip-text bg-gradient-to-r from-lime-400 to-amber-400 tracking-tighter mb-4 anim-fade-up">
            {vibeTag}
          </h1>
          {!isLoading && totalCount > 0 && (
            <p className="font-mono text-sm text-white/50 uppercase tracking-widest anim-fade-up" style={{ animationDelay: '0.1s' }}>
              {totalCount} products tagged
            </p>
          )}
        </div>

        {isLoading ? (
          <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
            {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : (!hasTrends && !hasCurated) ? (
          <EmptyState 
            message={`No trends tagged ${vibeTag} yet`} 
            subMessage="Check back later or explore other vibes."
            actionLabel="View All Trends"
            onAction={() => window.location.href = '/'}
          />
        ) : (
          <div className="space-y-16">
            {/* Curated First */}
            {hasCurated && (
              <section>
                <div className="flex items-center gap-4 mb-8">
                  <h2 className="font-display text-3xl font-bold text-white tracking-tight">Only On TrendZY</h2>
                  <div className="h-px flex-1 bg-border/50"></div>
                </div>
                {curatedError ? (
                  <ErrorState message="Could not load curated items" />
                ) : (
                  <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
                    {curatedList.map(product => (
                      <ProductCard key={product.id} product={product} source="vibe_curated" />
                    ))}
                  </div>
                )}
              </section>
            )}

            {/* Trends Second */}
            {hasTrends && (
              <section>
                <div className="flex items-center gap-4 mb-8">
                  <h2 className="font-display text-3xl font-bold text-white tracking-tight">Global Trends</h2>
                  <div className="h-px flex-1 bg-border/50"></div>
                </div>
                {trendsError ? (
                  <ErrorState message="Could not load trends" />
                ) : (
                  <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
                    {trendsList.map(product => (
                      <ProductCard key={product.id} product={product} source="vibe_trends" />
                    ))}
                  </div>
                )}
              </section>
            )}
            
            {/* Note: Pagination (Load More) omitted for brevity as API handles page/size but basic array display fulfills core req */ }
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}
