import { create } from 'zustand';

const useAuthStore = create((set) => ({
  token: localStorage.getItem('trendzy_token'),
  user: null,
  isLoggedIn: !!localStorage.getItem('trendzy_token'),
  showAuthModal: false,

  login: (token, user) => {
    localStorage.setItem('trendzy_token', token);
    set({ token, user, isLoggedIn: true, showAuthModal: false });
  },

  logout: () => {
    localStorage.removeItem('trendzy_token');
    set({ token: null, user: null, isLoggedIn: false });
  },

  openAuthModal: () => set({ showAuthModal: true }),
  closeAuthModal: () => set({ showAuthModal: false }),
}));

export default useAuthStore;
