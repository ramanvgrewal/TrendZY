export default function Footer() {
  return (
    <footer className="border-t border-border mt-20 pt-12 pb-8 bg-card">
      <div className="max-w-7xl mx-auto px-4 text-center">
        <h2 className="font-display font-black text-3xl tracking-tighter text-white/20 mb-4">
          TrendZY
        </h2>
        <p className="font-mono text-sm text-white/40 mb-8 max-w-sm mx-auto leading-relaxed">
          AI-powered trend intelligence for Indian Gen-Z consumers. Discover what's blowing up before everyone else does.
        </p>
        <div className="flex items-center justify-center gap-6 font-mono text-xs uppercase tracking-widest text-white/50">
          <a href="#" className="hover:text-white transition-colors">Terms</a>
          <a href="#" className="hover:text-white transition-colors">Privacy</a>
          <a href="#" className="hover:text-white transition-colors">Instagram</a>
          <a href="#" className="hover:text-white transition-colors">Contact</a>
        </div>
      </div>
    </footer>
  );
}
