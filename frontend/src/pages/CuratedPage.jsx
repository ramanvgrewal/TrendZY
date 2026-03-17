import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getFeaturedCurated, getCuratedBrands, getCurated } from '../api/curated';
import useFilterStore from '../store/filterStore';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import ProductCard from '../components/ui/ProductCard';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';
import VelocityBadge from '../components/ui/VelocityBadge';

export default function CuratedPage() {
  const { activeVibe } = useFilterStore();
  const [activeBrand, setActiveBrand] = useState(null);

  const { data: featuredData, isLoading: featuredLoading, isError: featuredError } = useQuery({
    queryKey: ['featured'],
    queryFn: getFeaturedCurated
  });

  const { data: brandsData, isLoading: brandsLoading, isError: brandsError } = useQuery({
    queryKey: ['brands'],
    queryFn: getCuratedBrands
  });

  const vibeParam = activeVibe === 'All' ? null : activeVibe;

  const { data: curatedData, isLoading: curatedLoading, isError: curatedError, refetch } = useQuery({
    queryKey: ['curated', activeBrand, activeVibe],
    queryFn: () => getCurated(vibeParam, activeBrand, 0, 20)
  });

  // Safe data extraction
  const brands = Array.isArray(brandsData) ? brandsData : [];
  const products = curatedData?.content || (Array.isArray(curatedData) ? curatedData : []);
  const hero = featuredData || null;

  return (
    <div className="min-h-screen flex flex-col bg-bg">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 pt-10 pb-20">
        
        <div className="text-center mb-12">
          <h1 className="font-display font-black text-5xl md:text-6xl text-white tracking-tighter mb-4">
            ONLY ON <span className="text-violet-400">TRENDZY</span>
          </h1>
          <p className="font-mono text-sm text-white/50 max-w-xl mx-auto">
            A hand-picked selection of indie drops, underground brands, and premium finds you won't see anywhere else.
          </p>
        </div>

        {/* FEATURED SPOTLIGHT */}
        {featuredLoading ? (
          <div className="w-full h-[400px] bg-[#1a1a1a] animate-pulse rounded-2xl mb-16"></div>
        ) : featuredError || !hero ? null : (
          <div className="relative w-full h-[400px] rounded-2xl overflow-hidden border border-violet-400/30 group mb-16">
            {hero.imageUrl ? (
              <div 
                className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105"
                style={{ backgroundImage: `url(${hero.imageUrl})` }}
              ></div>
            ) : (
              <div className="absolute inset-0 bg-gradient-to-br from-violet-900/40 to-bg"></div>
            )}
            
            <div className="absolute inset-0 bg-gradient-to-t from-bg via-bg/80 to-transparent"></div>
            <div className="absolute inset-0 bg-gradient-to-r from-bg/90 via-bg/40 to-transparent"></div>

            <div className="absolute inset-0 p-8 md:p-12 flex flex-col justify-end">
              <div className="max-w-xl">
                <div className="flex flex-wrap items-center gap-3 mb-4">
                  <span className="bg-violet-400 text-black font-mono font-bold text-xs uppercase tracking-widest px-3 py-1 rounded">
                    Featured
                  </span>
                  {hero.velocityLabel && <VelocityBadge label={hero.velocityLabel} />}
                </div>

                <h2 className="font-display font-black text-4xl md:text-5xl text-white tracking-tight mb-2 leading-none">
                  {hero.productName}
                </h2>
                
                <p className="font-mono text-xs text-white/50 mb-6 max-w-md line-clamp-2">
                  {hero.aiSummary || hero.description || 'Exclusive drop available now.'}
                </p>

                <button 
                  onClick={() => window.location.href = `/product/${hero.id}?tier=curated`}
                  className="bg-white hover:bg-violet-400 text-black font-mono font-bold tracking-widest uppercase px-8 py-3 rounded-xl transition-all"
                >
                  VIEW DETAILS
                </button>
              </div>
            </div>
          </div>
        )}

        {/* BRANDS FILTER PILLS */}
        {(!brandsError && brandsData?.length > 0) && (
          <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide mb-10 border-b border-border/50">
            {/* All Brands — always first */}
            <button
              onClick={() => setActiveBrand(null)}
              className={`flex-shrink-0 px-4 py-2 rounded-full text-xs font-mono border transition-all duration-200 ${
                activeBrand === null
                  ? 'bg-[#c4b5fd] text-black border-[#c4b5fd]'
                  : 'bg-transparent text-white border-white/20 hover:border-white/40'
              }`}
            >
              ALL BRANDS
            </button>
            
            {brandsLoading ? (
              [...Array(4)].map((_, i) => <div key={i} className="h-8 w-24 bg-[#1a1a1a] animate-pulse rounded-full flex-shrink-0"></div>)
            ) : (
              brands.map((brand) => (
                <button
                  key={brand.brandName}
                  onClick={() => setActiveBrand(brand.brandName)}
                  className={`flex-shrink-0 px-4 py-2 rounded-full text-xs font-mono border transition-all duration-200 flex items-center gap-2 ${
                    activeBrand === brand.brandName
                      ? 'bg-[#c4b5fd] text-black border-[#c4b5fd]'
                      : 'bg-transparent text-white border-white/20 hover:border-white/40'
                  }`}
                >
                  {brand.logoUrl && (
                    <img src={brand.logoUrl} alt={brand.brandName}
                         className="h-4 w-4 object-contain rounded-full"
                         onError={(e) => e.currentTarget.style.display='none'} />
                  )}
                  {brand.brandName}
                  <span className="opacity-50">({brand.productCount})</span>
                </button>
              ))
            )}
          </div>
        )}

        {/* CURATED GRID */}
        {curatedLoading ? (
           <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
             {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
           </div>
        ) : curatedError ? (
           <ErrorState message="Failed to load curated products" onRetry={refetch} />
        ) : products.length > 0 ? (
           <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
             {products.map(product => (
               <ProductCard key={product.id} product={product} source="curated_feed" />
             ))}
           </div>
        ) : (
           <EmptyState 
             message={activeBrand ? `No products from ${activeBrand} yet` : "No curated products available yet"}
             subMessage="We're dropping new items soon."
           />
        )}
      </main>

      <Footer />
    </div>
  );
}
