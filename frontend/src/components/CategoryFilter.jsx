export default function CategoryFilter({ categories, active, onChange }) {
  return (
    <div className="scrollbar-hide flex gap-2 overflow-x-auto py-1">
      {categories.map((category) => {
        const selected = active === category;
        return (
          <button
            key={category}
            onClick={() => onChange(category)}
            className={`shrink-0 rounded-full border px-4 py-2 font-body text-xs font-medium uppercase tracking-wider transition-all duration-200 ${
              selected
                ? 'border-primary bg-primary text-background shadow-glow'
                : 'border-border bg-surface-elevated text-muted hover:border-secondary/50 hover:text-foreground'
            }`}
          >
            {category}
          </button>
        );
      })}
    </div>
  );
}
