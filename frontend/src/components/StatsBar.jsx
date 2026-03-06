const stats = [
  { icon: '??', label: 'Signals', value: 47 },
  { icon: '??', label: 'Trends', value: 10 },
  { icon: '????', label: 'India', value: 8 },
  { icon: '??', label: 'Subreddits', value: 24 },
];

export default function StatsBar() {
  return (
    <section className="-mt-8">
      <div className="scrollbar-hide flex gap-3 overflow-x-auto pb-1">
        {stats.map((stat) => (
          <article
            key={stat.label}
            className="card-hover min-w-[180px] rounded-xl border border-border bg-card p-4"
          >
            <p className="mb-1 font-body text-xs font-medium uppercase tracking-wider text-muted">
              {stat.icon} {stat.label}
            </p>
            <p className="font-display text-[2rem] font-bold leading-none text-foreground">{stat.value}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
