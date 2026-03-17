export default function EmptyState({ message, subMessage, actionLabel, onAction }) {
  return (
    <div className="w-full rounded-2xl border border-dashed border-border p-12 flex flex-col items-center justify-center text-center bg-surface/30 my-8">
      <div className="w-16 h-16 rounded-full bg-surface border border-border flex items-center justify-center mb-6 text-2xl text-white/20">
        Ø
      </div>
      <h3 className="font-display text-2xl text-white/80 mb-2">{message}</h3>
      {subMessage && (
        <p className="font-mono text-sm text-white/40 mb-6">{subMessage}</p>
      )}
      {onAction && actionLabel && (
        <button
          onClick={onAction}
          className="font-mono text-sm uppercase tracking-widest text-lime-400 hover:text-[#000] border border-lime-400 hover:bg-lime-400 px-6 py-2.5 rounded-full transition-all"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
