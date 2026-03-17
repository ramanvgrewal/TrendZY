import toast from 'react-hot-toast';

export default function ShareModal({ url, title, onClose }) {
  const handleCopyLink = () => {
    navigator.clipboard.writeText(url)
      .then(() => {
        toast.success("Link copied!");
        onClose();
      })
      .catch(() => {
        toast.error("Failed to copy link");
      });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4">
      <div 
        className="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      ></div>
      
      <div className="relative bg-card border border-border rounded-t-2xl sm:rounded-2xl shadow-card w-full max-w-sm overflow-hidden anim-fade-up">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h3 className="font-display font-bold text-xl text-white">Share Trend</h3>
            <button onClick={onClose} className="text-white/40 hover:text-white">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          
          <div className="flex flex-col gap-3">
            <button 
              onClick={handleCopyLink}
              className="w-full flex items-center justify-center gap-2 bg-surface hover:bg-surface/80 border border-border text-white font-mono text-sm py-3 rounded-xl transition-colors"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
              Copy Link
            </button>
            <a 
              href={`https://wa.me/?text=${encodeURIComponent(title + " " + url)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full flex items-center justify-center gap-2 bg-[#25D366]/20 hover:bg-[#25D366]/30 border border-[#25D366]/50 text-[#25D366] font-mono text-sm py-3 rounded-xl transition-colors"
            >
              WhatsApp
            </a>
            <a 
              href={`https://twitter.com/intent/tweet?text=${encodeURIComponent("Check out this trend: " + title)}&url=${encodeURIComponent(url)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full flex items-center justify-center gap-2 bg-[#1DA1F2]/20 hover:bg-[#1DA1F2]/30 border border-[#1DA1F2]/50 text-[#1DA1F2] font-mono text-sm py-3 rounded-xl transition-colors"
            >
              Twitter / X
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
