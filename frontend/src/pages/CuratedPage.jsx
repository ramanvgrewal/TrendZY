import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getFeaturedCurated, getCuratedBrands, getCurated } from '../api/curated';
import useFilterStore from '../store/filterStore';
import { groupByProductType } from '../utils/productTypeClassifier';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import CategoryRow from '../components/ui/CategoryRow';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';
import VelocityBadge from '../components/ui/VelocityBadge';

function SkeletonSection() {
  return (
      <div style={{ marginBottom: 36 }}>
        <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 12px', display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ width: 3, height: 40, borderRadius: 3, background: 'hsl(0 0% 16%)' }} />
          <div style={{ width: 24, height: 24, borderRadius: 4, background: 'hsl(0 0% 16%)' }} />
          <div>
            <div style={{ width: 150, height: 16, borderRadius: 4, background: 'hsl(0 0% 16%)', marginBottom: 5 }} className="anim-shimmer" />
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

export default function CuratedPage() {
  const { activeVibe } = useFilterStore();
  const [activeBrand, setActiveBrand] = useState(null);
  const vibeParam = activeVibe === 'All' ? null : activeVibe;

  const { data: featuredData, isLoading: featuredLoading } =
      useQuery({ queryKey: ['featured'], queryFn: getFeaturedCurated });

  const { data: brandsData, isLoading: brandsLoading } =
      useQuery({ queryKey: ['brands'], queryFn: getCuratedBrands });

  const { data: curatedData, isLoading, isError, refetch } = useQuery({
    queryKey: ['curated', activeBrand, activeVibe],
    queryFn: () => getCurated(vibeParam, activeBrand, 0, 100),
  });

  const brands = Array.isArray(brandsData) ? brandsData : [];
  const hero   = Array.isArray(featuredData) ? featuredData[0] : featuredData;
  const typeGroups = useMemo(() => groupByProductType(curatedData), [curatedData]);

  return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
        <Navbar />

        <main style={{ flex: 1 }}>

          {/* ════ PAGE HEADER ════ */}
          <div style={{ maxWidth: 1280, margin: '0 auto', padding: '40px 16px 32px', textAlign: 'center' }}>
          <span className="font-mono" style={{
            fontSize: 10, fontWeight: 700, letterSpacing: '0.18em', textTransform: 'uppercase',
            padding: '4px 14px', borderRadius: 6,
            background: 'rgba(196,181,253,0.12)', color: '#c4b5fd',
            border: '1px solid rgba(196,181,253,0.25)',
            display: 'inline-block', marginBottom: 16,
          }}>
            ✦ Curated Collection
          </span>
            <h1 className="font-display" style={{
              fontSize: 'clamp(36px, 6vw, 64px)', fontWeight: 900,
              letterSpacing: '-0.02em', lineHeight: 1, margin: '0 0 10px', color: '#f5f5f5',
            }}>
              ONLY ON <span style={{ color: '#c4b5fd' }}>TRENDZY</span>
            </h1>
            <p className="font-mono" style={{
              fontSize: 12, color: 'rgba(255,255,255,0.4)', maxWidth: 500, margin: '0 auto', lineHeight: 1.6,
            }}>
              Hand-picked drops from indie brands and hidden gems you won't find on Amazon or Myntra.
            </p>
          </div>

          {/* ════ HERO SPOTLIGHT ════ */}
          {!featuredLoading && hero && (
              <div style={{ maxWidth: 1280, margin: '0 auto 40px', padding: '0 16px' }}>
                <div
                    style={{
                      position: 'relative', width: '100%', minHeight: 360,
                      borderRadius: 18, overflow: 'hidden',
                      border: '1px solid rgba(196,181,253,0.2)',
                    }}
                    className="group"
                >
                  {hero.imageUrl && (
                      <>
                        <img
                            src={hero.imageUrl} alt={hero.productName}
                            style={{
                              position: 'absolute', inset: 0, width: '100%', height: '100%',
                              objectFit: 'cover', objectPosition: 'top center',
                              transition: 'transform 700ms ease',
                            }}
                            className="group-hover:scale-105"
                            onError={(e) => (e.currentTarget.style.display = 'none')}
                        />
                        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to right, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.5) 60%, rgba(0,0,0,0.1) 100%)' }} />
                        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 50%)' }} />
                      </>
                  )}
                  {!hero.imageUrl && (
                      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(135deg, rgba(196,181,253,0.08) 0%, hsl(0 0% 9%) 100%)' }} />
                  )}

                  <div style={{
                    position: 'relative', padding: 'clamp(28px, 5vw, 52px)',
                    display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', minHeight: 360,
                  }}>
                    <div style={{ maxWidth: 480 }}>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
                    <span className="font-mono" style={{
                      fontSize: 10, fontWeight: 700, letterSpacing: '0.14em',
                      textTransform: 'uppercase', padding: '3px 12px', borderRadius: 5,
                      background: '#c4b5fd', color: '#000',
                    }}>✦ Featured Drop</span>
                        {hero.velocityLabel && <VelocityBadge label={hero.velocityLabel} />}
                      </div>
                      {hero.brandName && (
                          <p className="font-mono" style={{ fontSize: 11, fontWeight: 700, color: '#c4b5fd', textTransform: 'uppercase', letterSpacing: '0.14em', marginBottom: 6 }}>
                            {hero.brandName}
                          </p>
                      )}
                      <h2 className="font-display" style={{
                        fontSize: 'clamp(32px, 5vw, 56px)', fontWeight: 900,
                        color: '#f5f5f5', lineHeight: 0.95, letterSpacing: '-0.01em', marginBottom: 14,
                      }}>
                        {hero.productName}
                      </h2>
                      {(hero.aiSummary || hero.description) && (
                          <p className="font-mono" style={{
                            fontSize: 11, color: 'rgba(255,255,255,0.45)', lineHeight: 1.6, marginBottom: 20,
                            display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                          }}>
                            {hero.aiSummary || hero.description}
                          </p>
                      )}
                      <button
                          onClick={() => window.location.href = `/product/${hero.id}?tier=curated`}
                          className="font-mono"
                          style={{
                            fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase',
                            padding: '12px 28px', borderRadius: 12, background: '#f5f5f5', color: '#000',
                            border: 'none', cursor: 'pointer', transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => (e.currentTarget.style.background = '#c4b5fd')}
                          onMouseLeave={(e) => (e.currentTarget.style.background = '#f5f5f5')}
                      >
                        VIEW DETAILS →
                      </button>
                    </div>
                  </div>
                </div>
              </div>
          )}

          {/* ════ BRAND FILTER PILLS ════ */}
          {brands.length > 0 && (
              <div style={{ borderBottom: '1px solid hsl(0 0% 14%)', marginBottom: 36 }}>
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px 14px', display: 'flex', gap: 8, overflowX: 'auto' }} className="scrollbar-hide">
                  {[{ brandName: null, label: 'ALL BRANDS' }, ...brands.map(b => ({ ...b, label: b.brandName }))].map((brand) => (
                      <button
                          key={brand.brandName ?? '__all__'}
                          onClick={() => setActiveBrand(brand.brandName)}
                          className="font-mono"
                          style={{
                            flexShrink: 0, fontSize: 10, fontWeight: 700,
                            letterSpacing: '0.1em', textTransform: 'uppercase',
                            padding: '6px 16px', borderRadius: 20, cursor: 'pointer',
                            display: 'flex', alignItems: 'center', gap: 6,
                            border: `1px solid ${activeBrand === brand.brandName ? '#c4b5fd' : 'rgba(255,255,255,0.15)'}`,
                            background: activeBrand === brand.brandName ? '#c4b5fd' : 'transparent',
                            color: activeBrand === brand.brandName ? '#000' : 'rgba(255,255,255,0.6)',
                            transition: 'all 0.2s',
                          }}
                      >
                        {brand.logoUrl && (
                            <img src={brand.logoUrl} alt={brand.label} style={{ width: 14, height: 14, objectFit: 'contain', borderRadius: '50%' }}
                                 onError={(e) => (e.currentTarget.style.display = 'none')} />
                        )}
                        {brand.label}
                        {brand.productCount && <span style={{ opacity: 0.5 }}>({brand.productCount})</span>}
                      </button>
                  ))}
                </div>
              </div>
          )}

          {/* ════ PRODUCTS by type ════ */}
          <div style={{ paddingBottom: 48 }}>
            {/* Sub-heading */}
            <div style={{ maxWidth: 1280, margin: '0 auto 20px', padding: '0 16px' }}>
            <span className="font-mono" style={{
              fontSize: 10, fontWeight: 700, letterSpacing: '0.16em', textTransform: 'uppercase',
              padding: '3px 10px', borderRadius: 5,
              background: 'rgba(196,181,253,0.12)', color: '#c4b5fd',
              border: '1px solid rgba(196,181,253,0.25)',
            }}>
              Browse by Product Type
            </span>
            </div>

            {isLoading ? (
                <><SkeletonSection /><SkeletonSection /><SkeletonSection /></>
            ) : isError ? (
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px' }}>
                  <ErrorState message="Failed to load curated products" onRetry={refetch} />
                </div>
            ) : typeGroups.length > 0 ? (
                typeGroups.map(({ cfg, products }) => (
                    <CategoryRow key={`crt-${cfg.key}`} cfg={cfg} products={products} source="curated_page" />
                ))
            ) : (
                <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 16px' }}>
                  <EmptyState
                      message={activeBrand ? `No products from ${activeBrand} yet` : 'No curated products yet'}
                      subMessage="We're dropping new items soon. Check back later."
                  />
                </div>
            )}
          </div>
        </main>

        <Footer />
      </div>
  );
}