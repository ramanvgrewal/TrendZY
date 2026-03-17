import { create } from 'zustand';

const useFilterStore = create((set) => ({
  activeVibe: 'All',
  setVibe: (vibe) => set({ activeVibe: vibe }),
}));

export default useFilterStore;
