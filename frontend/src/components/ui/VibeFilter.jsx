import useFilterStore from '../../store/filterStore';

const VIBE_TAGS = ['All', '#GenZ', '#Y2K', '#Streetwear', '#Cottagecore', '#CleanGirl', '#Indie', '#Gorpcore', '#Dopamine'];

export default function VibeFilter({ onChange }) {
  const { activeVibe, setVibe } = useFilterStore();

  const handleVibeClick = (vibe) => {
    setVibe(vibe);
    if (onChange) onChange(vibe);
  };

  return (
    <div className="w-full overflow-x-auto scrollbar-hide py-4 border-b border-border bg-bg/50 backdrop-blur-sm sticky top-16 z-30">
      <div className="max-w-7xl mx-auto px-4 flex items-center gap-3 w-max">
        {VIBE_TAGS.map((vibe) => (
          <button
            key={vibe}
            onClick={() => handleVibeClick(vibe)}
            className={`font-mono text-sm px-5 py-2 rounded-full transition-all tracking-wide ${
              activeVibe === vibe
                ? 'bg-lime-400 text-black font-semibold shadow-[0_0_15px_rgba(163,230,53,0.3)]'
                : 'bg-surface border border-border text-white/60 hover:text-white hover:border-lime-400/50 hover:bg-lime-400/10'
            }`}
          >
            {vibe}
          </button>
        ))}
      </div>
    </div>
  );
}
