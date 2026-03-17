import { useState } from 'react';
import useAuthStore from '../../store/authStore';
import { login, register } from '../../api/auth';
import toast from 'react-hot-toast';

export default function AuthModal() {
  const { closeAuthModal, login: setGlobalLogin } = useAuthStore();
  const [activeTab, setActiveTab] = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      let data;
      if (activeTab === 'login') {
        data = await login(email, password);
      } else {
        data = await register(name, email, password);
      }
      setGlobalLogin(data.token, data.user);
      toast.success(`Welcome ${activeTab === 'register' ? 'to TrendZY' : 'back'}!`);
      closeAuthModal();
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={closeAuthModal}
      ></div>
      
      {/* Modal Box */}
      <div className="relative bg-card border border-border rounded-2xl shadow-card w-full max-w-md overflow-hidden anim-fade-up">
        <button 
          onClick={closeAuthModal}
          className="absolute top-4 right-4 text-white/40 hover:text-white transition-colors"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div className="p-8">
          <div className="text-center mb-8">
            <h2 className="font-display font-black text-3xl tracking-tight text-white mb-2">
              Join TrendZY
            </h2>
            <p className="font-mono text-sm text-white/50">
              Save products. Get alerts. Catch the wave.
            </p>
          </div>

          <div className="flex p-1 mb-6 bg-surface rounded-xl">
            <button 
              onClick={() => { setActiveTab('login'); setError(null); }}
              className={`flex-1 py-2 font-mono text-sm font-medium rounded-lg transition-all ${activeTab === 'login' ? 'bg-card text-white shadow' : 'text-white/40 hover:text-white/80'}`}
            >
              Sign In
            </button>
            <button 
              onClick={() => { setActiveTab('register'); setError(null); }}
              className={`flex-1 py-2 font-mono text-sm font-medium rounded-lg transition-all ${activeTab === 'register' ? 'bg-card text-white shadow' : 'text-white/40 hover:text-white/80'}`}
            >
              Create Account
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {activeTab === 'register' && (
              <div className="space-y-1">
                <label className="label">Name</label>
                <input 
                  type="text" 
                  required
                  value={name}
                  onChange={e => setName(e.target.value)}
                  className="w-full bg-surface border border-border rounded-xl px-4 py-3 text-white font-mono text-sm placeholder:text-white/20 focus:outline-none focus:border-lime-400 transition-colors"
                  placeholder="How should we call you?"
                />
              </div>
            )}
            <div className="space-y-1">
              <label className="label">Email</label>
              <input 
                type="email" 
                required
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full bg-surface border border-border rounded-xl px-4 py-3 text-white font-mono text-sm placeholder:text-white/20 focus:outline-none focus:border-lime-400 transition-colors"
                placeholder="you@email.com"
              />
            </div>
            <div className="space-y-1">
              <label className="label">Password</label>
              <input 
                type="password" 
                required
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full bg-surface border border-border rounded-xl px-4 py-3 text-white font-mono text-sm placeholder:text-white/20 focus:outline-none focus:border-lime-400 transition-colors"
                placeholder="••••••••"
              />
            </div>

            {error && (
              <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
                <p className="font-mono text-xs text-red-400 text-center">{error}</p>
              </div>
            )}

            <button 
              type="submit"
              disabled={loading}
              className="w-full bg-lime-400 hover:bg-lime-500 text-black font-mono font-bold uppercase tracking-widest py-3.5 rounded-xl transition-colors mt-6 flex justify-center items-center"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-black/20 border-t-black rounded-full animate-spin"></div>
              ) : (
                activeTab === 'login' ? 'Sign In' : 'Create Account'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
