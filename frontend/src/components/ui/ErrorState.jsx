export default function ErrorState({ message = "Something went wrong", onRetry }) {
  return (
    <div className="w-full rounded-2xl border border-red-500/20 p-8 flex flex-col items-center justify-center text-center bg-red-500/5 my-8">
      <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center mb-4 text-red-400 font-mono font-bold text-xl">
        !
      </div>
      <p className="font-mono text-red-300 mb-6">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="font-mono text-sm uppercase tracking-widest text-red-400 hover:text-white border border-red-500/30 hover:border-red-400 hover:bg-red-500/20 px-6 py-2 rounded-full transition-all"
        >
          Retry
        </button>
      )}
    </div>
  );
}
