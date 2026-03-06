import { Link, useLocation } from 'react-router-dom';

const links = [
  { to: '/', label: 'Discover' },
  { to: '/rising', label: 'Rising ??' },
];

export default function Navbar({ searchTerm, onSearch }) {
  const location = useLocation();

  return (
    <header className="sticky top-0 z-50 border-b border-border/70 bg-background/80 backdrop-blur-xl">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2">
          <span className="font-display text-xl font-bold tracking-tight gradient-text">TrendZY</span>
          <span className="h-2 w-2 rounded-full bg-primary animate-pulse-soft" />
        </Link>

        <nav className="hidden items-center gap-2 sm:flex">
          {links.map((link) => {
            const active = location.pathname === link.to;
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`rounded-lg px-3 py-2 font-body text-sm font-medium transition-colors duration-200 ${
                  active ? 'text-primary' : 'text-muted hover:text-foreground'
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-2">
          <div className="relative hidden sm:block">
            <svg
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
            >
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input
              value={searchTerm}
              onChange={(e) => onSearch(e.target.value)}
              placeholder="Search trends"
              className="w-52 rounded-lg border border-border bg-card py-2 pl-9 pr-3 font-body text-sm text-foreground placeholder:text-muted transition-colors duration-200 focus:border-primary/45 focus:outline-none"
            />
          </div>
          <button className="rounded-lg border border-border bg-card px-3 py-2 font-body text-sm font-medium text-foreground transition-colors duration-200 hover:border-primary/40 hover:text-primary">
            ?? Personalise
          </button>
        </div>
      </div>
    </header>
  );
}
