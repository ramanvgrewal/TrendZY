import { BrowserRouter, Route, Routes, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import useAuthStore from './store/authStore';
import { Component } from 'react';

class ErrorBoundary extends Component {
  state = { hasError: false, error: null };
  
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#080808] flex flex-col items-center justify-center gap-4 px-4">
          <p className="text-white font-mono text-sm">Something crashed on this page</p>
          <p className="text-[rgba(255,255,255,0.4)] font-mono text-xs max-w-md text-center">{this.state.error?.message}</p>
          <button 
            onClick={() => window.location.href = '/'}
            className="px-6 py-3 bg-[#a3e635] text-black font-bold text-sm rounded-xl">
            GO HOME
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

// Modals
import AuthModal from './components/modals/AuthModal';

// Pages
import HomePage from './pages/HomePage';
import ProductDetailPage from './pages/ProductDetailPage';
import SearchPage from './pages/SearchPage';
import VibeFilterPage from './pages/VibeFilterPage';
import CuratedPage from './pages/CuratedPage';
import SavedPage from './pages/SavedPage';

export default function App() {
  const { showAuthModal } = useAuthStore();

  return (
    <BrowserRouter>
      {/* Global Modals & Toasts */}
      {showAuthModal && <AuthModal />}
      <Toaster 
        position="top-right" 
        toastOptions={{
          style: {
            background: '#0d0d0d',
            color: '#fff',
            border: '1px solid rgba(255,255,255,0.1)',
            fontFamily: "'DM Mono', monospace",
            fontSize: '12px',
          }
        }} 
      />

      <ErrorBoundary>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/product/:id" element={<ProductDetailPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/vibe/:tag" element={<VibeFilterPage />} />
          <Route path="/curated" element={<CuratedPage />} />
          <Route path="/saved" element={<SavedPage />} />
          
          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ErrorBoundary>
    </BrowserRouter>
  );
}
