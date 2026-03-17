import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { searchProducts, getAutocomplete, getTrendingQueries } from '../api/search';
import useFilterStore from '../store/filterStore';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import ProductCard from '../components/ui/ProductCard';
import VibeFilter from '../components/ui/VibeFilter';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';

export default function SearchPage() {
  const navigate = useNavigate();
  const { activeVibe } = useFilterStore();
  
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [type, setType] = useState('all'); // 'all' | 'trending' | 'rising' | 'curated'
  
  const [showFilters, setShowFilters] = useState(false);
  const [showAutocomplete, setShowAutocomplete] = useState(false);

  // Debounce logic
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(query);
      if (query.length >= 2 && query !== submittedQuery) {
        setShowAutocomplete(true);
      } else {
        setShowAutocomplete(false);
      }
    }, 300);
    return () => clearTimeout(handler);
  }, [query, submittedQuery]);

  // Queries
  const { data: trendingQueries, isLoading: tqLoading, isError: tqError } = useQuery({
    queryKey: ['trendingQueries'],
    queryFn: getTrendingQueries,
    enabled: !submittedQuery && query.length === 0
  });

  const { data: autocompleteResults, isLoading: acLoading } = useQuery({
    queryKey: ['autocomplete', debouncedQuery],
    queryFn: () => getAutocomplete(debouncedQuery),
    enabled: debouncedQuery.length >= 2 && showAutocomplete
  });

  const vibeParam = activeVibe === 'All' ? null : activeVibe;

  const { data: searchResults, isLoading: searchLoading, isError: searchError, refetch: refetchSearch } = useQuery({
    queryKey: ['search', submittedQuery, type, activeVibe],
    queryFn: () => searchProducts(submittedQuery, type === 'all' ? null : type, vibeParam, 0),
    enabled: !!submittedQuery
  });

  const handleSearchSubmit = (e) => {
    e?.preventDefault();
    if (!query.trim()) return;
    setSubmittedQuery(query);
    setShowAutocomplete(false);
    setShowFilters(true); // show filters after first search
  };

  const handleChipClick = (q) => {
    setQuery(q);
    setSubmittedQuery(q);
    setShowAutocomplete(false);
    setShowFilters(true);
  };

  return (
    <div className="min-h-screen flex flex-col bg-bg">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 pt-10 pb-20">
        
        {/* SEARCH HEADER */}
        <div className="max-w-3xl mx-auto mb-12">
          <h1 className="font-display font-black text-4xl sm:text-5xl text-white tracking-tight mb-8 text-center">
            Find the Next Big Thing
          </h1>
          
          <form onSubmit={handleSearchSubmit} className="relative z-20">
            <div className={`relative flex items-center w-full bg-surface border transition-colors ${showAutocomplete ? 'border-lime-400 rounded-t-2xl' : 'border-border rounded-full hover:border-white/30 focus-within:border-lime-400'}`}>
              <div className="pl-6 text-white/50">
                <svg width="24" height="24" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <input 
                type="text" 
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search products, brands, vibes..."
                className="w-full bg-transparent border-none outline-none py-4 px-4 text-white font-mono text-lg placeholder:text-white/20"
                autoFocus
              />
              {query && (
                <button 
                  type="button" 
                  onClick={() => { setQuery(''); setSubmittedQuery(''); setShowFilters(false); }}
                  className="pr-6 text-white/40 hover:text-white"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"></path></svg>
                </button>
              )}
            </div>

            {/* Autocomplete Dropdown */}
            {showAutocomplete && (
              <div className="absolute top-full left-0 w-full bg-surface border-x border-b border-lime-400 rounded-b-2xl overflow-hidden shadow-2xl">
                {acLoading ? (
                  <div className="p-6 flex justify-center">
                    <div className="w-5 h-5 border-2 border-lime-400/20 border-t-lime-400 rounded-full animate-spin"></div>
                  </div>
                ) : autocompleteResults && autocompleteResults.length > 0 ? (
                  <ul>
                    {autocompleteResults.map((res, i) => (
                      <li key={i}>
                        <button 
                          type="button"
                          onClick={() => handleChipClick(res)}
                          className="w-full text-left px-6 py-3 font-mono text-sm text-white/80 hover:bg-white/5 hover:text-lime-400 transition-colors border-t border-border/50 first:border-none"
                        >
                          {res}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>
            )}
          </form>

          {/* Trending Suggestions (Before Search) */}
          {!submittedQuery && query.length === 0 && !tqError && (
            <div className="mt-8 text-center anim-fade-up">
              <p className="font-mono text-xs text-white/30 uppercase tracking-widest mb-4">Trending Searches</p>
              <div className="flex flex-wrap items-center justify-center gap-2">
                {tqLoading ? (
                  [...Array(5)].map((_, i) => <div key={i} className="h-8 w-24 bg-[#1a1a1a] animate-pulse rounded-full"></div>)
                ) : trendingQueries && trendingQueries.length > 0 ? (
                  trendingQueries.map((q, i) => (
                    <button 
                      key={i}
                      onClick={() => handleChipClick(q)}
                      className="font-mono text-xs text-white/60 bg-surface border border-border px-4 py-1.5 rounded-full hover:border-lime-400 hover:text-lime-400 transition-colors"
                    >
                      {q}
                    </button>
                  ))
                ) : null}
              </div>
            </div>
          )}
        </div>

        {/* FILTERS TABS */}
        {showFilters && (
          <div className="anim-fade-in mb-8">
            <div className="flex justify-center border-b border-border/50 mb-6">
              <div className="flex gap-8 overflow-x-auto scrollbar-hide py-[2px]">
                {['all', 'trending', 'rising', 'curated'].map(t => (
                  <button
                    key={t}
                    onClick={() => setType(t)}
                    className={`font-mono text-sm uppercase tracking-widest pb-3 border-b-2 transition-colors whitespace-nowrap ${type === t ? 'border-lime-400 text-lime-400 font-bold' : 'border-transparent text-white/40 hover:text-white'}`}
                  >
                    {t === 'curated' ? 'Only On TrendZY' : t}
                  </button>
                ))}
              </div>
            </div>
            
            <VibeFilter />
          </div>
        )}

        {/* SEARCH RESULTS */}
        {submittedQuery && (
          <div className="mt-8">
            <h2 className="font-display text-2xl text-white mb-6">
              Results for <span className="text-lime-400">"{submittedQuery}"</span>
            </h2>

            {searchLoading ? (
              <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
                {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
              </div>
            ) : searchError ? (
              <ErrorState message="Failed to fetch search results." onRetry={refetchSearch} />
            ) : searchResults?.content?.length > 0 || searchResults?.length > 0 ? (
               <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
                 {(searchResults.content || searchResults).map(product => (
                   <ProductCard key={product.id} product={product} source="search_results" />
                 ))}
               </div>
            ) : (
              <EmptyState 
                message={`No results for "${submittedQuery}"`}
                subMessage="Try adjusting your vibes or searching for something else."
              />
            )}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}
