import SkeletonRow from './SkeletonRow';
import ErrorState from './ErrorState';
import ProductCard from './ProductCard';
import { useRef } from 'react';

export default function ScrollRow({ title, subtitle, badge, badgeColor = 'lime', isLoading, isError, data, featured, onRetry, source = "home_feed" }) {
  const scrollRef = useRef(null);

  const items = data?.content || (Array.isArray(data) ? data : []);

  // Add hide logic for empty data (only if not loading and not error)
  if (!isLoading && !isError && items.length === 0) return null;

  const scrollLeft = () => scrollRef.current?.scrollBy({ left: -600, behavior: 'smooth' });
  const scrollRight = () => scrollRef.current?.scrollBy({ left: 600, behavior: 'smooth' });

  // Add color specific classes for badges/accents
  const getBadgeColorClass = () => {
    switch(badgeColor) {
      case 'amber': return 'text-amber-400 bg-amber-400/10 border-amber-400/20';
      case 'purple': return 'text-violet-400 bg-violet-400/10 border-violet-400/20';
      default: return 'text-lime-400 bg-lime-400/10 border-lime-400/20';
    }
  };

  return (
    <section className="my-16">
      <div className="max-w-7xl mx-auto px-4 mb-6 flex items-end justify-between">
        <div>
          {badge && (
            <span className={`inline-block font-mono text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded border mb-3 ${getBadgeColorClass()}`}>
              {badge}
            </span>
          )}
          <h2 className="font-display text-4xl font-black tracking-tight text-white">{title}</h2>
          {subtitle && <p className="font-mono text-sm text-white/50 mt-1">{subtitle}</p>}
        </div>
        
        {!isLoading && !isError && items.length > 0 && (
          <div className="hidden md:flex items-center gap-2">
            <button onClick={scrollLeft} className="w-10 h-10 rounded-full border border-border flex items-center justify-center text-white/50 hover:text-white hover:border-lime-400 transition-colors bg-surface">
              ←
            </button>
            <button onClick={scrollRight} className="w-10 h-10 rounded-full border border-border flex items-center justify-center text-white/50 hover:text-white hover:border-lime-400 transition-colors bg-surface">
              →
            </button>
          </div>
        )}
      </div>

      {isLoading && (
        <div className="max-w-7xl mx-auto px-4">
          <SkeletonRow />
        </div>
      )}

      {isError && (
        <div className="max-w-7xl mx-auto px-4">
          <ErrorState onRetry={onRetry} />
        </div>
      )}

      {!isLoading && !isError && items.length > 0 && (
        <div className="relative w-full overflow-hidden">
          <div 
            ref={scrollRef} 
            className="flex gap-6 overflow-x-auto scrollbar-hide px-4 max-w-7xl mx-auto pb-8 snap-x"
          >
            {items.map(product => (
              <div key={product.id} className="snap-start flex-shrink-0 w-[220px]">
                <ProductCard product={product} featured={featured} source={source || 'scroll_row'} />
              </div>
            ))}
          </div>
          
          {/* Right gradient fade indicator */}
          <div className="absolute right-0 top-0 bottom-8 w-16 bg-gradient-to-l from-bg to-transparent pointer-events-none"></div>
        </div>
      )}
    </section>
  );
}
