import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getSaved } from '../api/user';
import useAuthStore from '../store/authStore';

import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import ProductCard from '../components/ui/ProductCard';
import SkeletonCard from '../components/ui/SkeletonCard';
import ErrorState from '../components/ui/ErrorState';
import EmptyState from '../components/ui/EmptyState';
import { filterAndSortProducts } from '../utils/productUtils';

export default function SavedPage() {
  const navigate = useNavigate();
  const { isLoggedIn, openAuthModal } = useAuthStore();

  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/');
      openAuthModal();
    }
  }, [isLoggedIn, navigate, openAuthModal]);

  const { data: rawSavedItems, isLoading, isError, refetch } = useQuery({
    queryKey: ['saved'],
    queryFn: getSaved,
    enabled: isLoggedIn
  });

  const savedItems = filterAndSortProducts(rawSavedItems);

  if (!isLoggedIn) return null; // Wait for redirect

  return (
    <div className="min-h-screen flex flex-col bg-bg">
      <Navbar />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 pt-16 pb-20">
        <div className="mb-12">
          <h1 className="font-display font-black text-4xl sm:text-5xl text-white tracking-tight mb-2">
            Saved Drops
          </h1>
          <p className="font-mono text-sm text-white/50">
            Your personal watchlist of trends and curators.
          </p>
        </div>

        {isLoading ? (
          <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
            {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : isError ? (
          <ErrorState message="Could not load your saved items" onRetry={refetch} />
        ) : savedItems.length > 0 ? (
          <div className="flex flex-wrap gap-6 justify-center sm:justify-start">
            {savedItems.map(product => (
              <ProductCard key={product.id} product={product} source="saved_feed" />
            ))}
          </div>
        ) : (
          <EmptyState 
            message="No saved items yet"
            subMessage="Heart products to save them here."
            actionLabel="Discover Trends"
            onAction={() => navigate('/')}
          />
        )}
      </main>

      <Footer />
    </div>
  );
}
