import { Link, useNavigate } from 'react-router-dom';
import useAuthStore from '../../store/authStore';
import { useState } from 'react';

export default function Navbar() {
  const navigate = useNavigate();
  const { isLoggedIn, user, openAuthModal, logout } = useAuthStore();
  const [showDropdown, setShowDropdown] = useState(false);

  const handleDropdownClick = (action) => {
    setShowDropdown(false);
    if (action === 'saved') {
      navigate('/saved');
    } else if (action === 'logout') {
      logout();
      navigate('/');
    }
  };

  return (
    <nav className="sticky top-0 z-40 bg-bg/80 backdrop-blur-md border-b border-border">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        
        {/* Left: Logo & Links */}
        <div className="flex items-center gap-8">
          <Link to="/" className="font-display font-black text-2xl tracking-tighter text-white">
            Trend<span className="text-lime-400">ZY</span>
          </Link>
          
          <div className="hidden md:flex items-center gap-6 font-mono text-sm uppercase tracking-wider text-white/50">
            <Link to="/?tier=trending" className="hover:text-white transition-colors">Trending</Link>
            <Link to="/?tier=rising" className="hover:text-white transition-colors">Rising</Link>
            <Link to="/curated" className="hover:text-white transition-colors">Only on TrendZY</Link>
          </div>
        </div>

        {/* Right: Search & Auth */}
        <div className="flex items-center gap-6">
          <button 
            onClick={() => navigate('/search')}
            className="text-white/50 hover:text-white transition-colors"
            aria-label="Search"
          >
            <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>

          {!isLoggedIn ? (
            <button 
              onClick={openAuthModal}
              className="font-mono text-sm uppercase tracking-widest text-white/80 hover:text-white border px-4 py-1.5 rounded-full border-white/20 hover:border-lime-400 hover:bg-lime-400/10 transition-all"
            >
              Sign In
            </button>
          ) : (
            <div className="relative">
              <button 
                onClick={() => setShowDropdown(!showDropdown)}
                className="w-8 h-8 rounded-full bg-surface border border-border flex items-center justify-center text-sm font-semibold hover:border-lime-400 transition-colors"
              >
                {user?.name?.charAt(0).toUpperCase() || 'U'}
              </button>
              
              {showDropdown && (
                <>
                  <div className="fixed inset-0 z-30" onClick={() => setShowDropdown(false)} />
                  <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-xl shadow-card z-40 overflow-hidden py-1 font-mono text-sm">
                    <button 
                      onClick={() => handleDropdownClick('saved')}
                      className="w-full text-left px-4 py-2 hover:bg-surface text-white/70 hover:text-white transition-colors"
                    >
                      Saved Items
                    </button>
                    <button 
                      onClick={() => handleDropdownClick('logout')}
                      className="w-full text-left px-4 py-2 hover:bg-surface text-red-400 hover:bg-red-500/10 transition-colors"
                    >
                      Sign Out
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
