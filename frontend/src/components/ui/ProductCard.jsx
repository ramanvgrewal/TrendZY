import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { getAffiliateLink, trackClick } from '../../api/affiliate';
import { saveProduct, unsaveProduct } from '../../api/user';
import useAuthStore from '../../store/authStore';
import ScoreRing from './ScoreRing';
import VelocityBadge from './VelocityBadge';

export default function ProductCard({ product, featured = false, source = "home_feed" }) {
  const navigate = useNavigate();
  const { isLoggedIn, openAuthModal } = useAuthStore();
  
  // Note: For a real app, isSaved would initialize based on user's saved items list.
  // We'll assume the API doesn't return isSaved strictly in the product list by default,
  // or it does. We'll just do local toggle for the optimistic update.
  const [isSaved, setIsSaved] = useState(false); 
  const [buyLoading, setBuyLoading] = useState(false);

  const [imgSrc, setImgSrc] = useState(
    product.primaryImageUrl || product.imageUrl || null
  );

  const imageLock = Math.abs(
    (product.productName || 'fashion')
      .split('')
      .reduce((acc, char) => acc + char.charCodeAt(0), 0) % 9999
  );
  const categoryStr = (product.category || 'fashion')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
  const fallbackUrl = `https://loremflickr.com/400/600/${categoryStr}?lock=${imageLock}`;

  const isCurated = product.platform === 'brand' || !!product.shopUrl;

  const handleCardClick = (e) => {
    // Prevent navigation if clicking inside buttons
    if (e.target.closest('button')) return;
    const tier = isCurated ? 'curated' : (product.tier || 'trending');
    navigate(`/product/${product.id}?tier=${tier}`);
  };

  const handleVibeClick = (e, tag) => {
    e.stopPropagation(); // don't trigger card navigation
    const cleanTag = tag.replace('#', '');
    navigate(`/vibe/${cleanTag}`);
  };

  const buyUrl = isCurated
    ? product.shopUrl
    : product.amazonUrl || product.myntraUrl || product.flipkartUrl;

  const buyLabel = isCurated
    ? `SHOP ${product.brandName?.toUpperCase() || 'NOW'}`
    : product.amazonUrl
      ? 'BUY ON AMAZON'
      : product.myntraUrl
        ? 'BUY ON MYNTRA'
        : 'BUY ON FLIPKART';

  const handleBuy = (e) => {
    e.stopPropagation(); // don't navigate to detail page
    if (!buyUrl) return;
    const platform = isCurated ? 'brand' 
                   : product.amazonUrl ? 'amazon' 
                   : product.myntraUrl ? 'myntra' 
                   : 'flipkart';
    window.open(buyUrl, '_blank', 'noopener,noreferrer');
    trackClick(product.id, platform, source);
  };

  const handleSave = async (e) => {
    e.stopPropagation();
    if (!isLoggedIn) {
      openAuthModal();
      return;
    }
    const wasSaved = isSaved;
    setIsSaved(!wasSaved);
    try {
      if (wasSaved) {
        await unsaveProduct(product.id);
        toast.success('Removed from saved');
      } else {
        await saveProduct(product.id);
        toast.success('Saved ♥');
      }
    } catch (err) {
      setIsSaved(wasSaved);
      toast.error('Could not update saved items.');
    }
  };

  return (
    <div 
      onClick={handleCardClick}
      className={`card-base card-hover cursor-pointer group flex flex-col ${featured ? 'w-[320px] md:w-[600px] h-auto md:h-[380px] md:flex-row' : 'w-[280px] h-[380px]'}`}
    >
      {/* Image Area */}
      <div className={`relative bg-[#1a1a1a] overflow-hidden flex-shrink-0 ${featured ? 'h-[280px] w-full md:h-full md:w-[300px]' : 'h-[280px] w-full'}`}>
        <img 
          src={imgSrc || fallbackUrl} 
          alt={product.productName} 
          className="w-full h-full object-cover object-center transition-transform duration-500 group-hover:scale-105"
          draggable={false}
          onError={() => setImgSrc(fallbackUrl)}
        />
        
        {/* Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-2">
          {!isCurated && product.tier && (
            <div className="bg-black/60 backdrop-blur px-2 py-0.5 rounded text-[10px] font-mono text-white/90 uppercase border border-white/10 w-fit">
              {product.tier}
            </div>
          )}
          {!isCurated && product.velocityLabel && <VelocityBadge label={product.velocityLabel} />}
        </div>
        
        <button 
          onClick={handleSave}
          className="absolute top-3 right-3 w-8 h-8 rounded-full bg-black/40 backdrop-blur flex items-center justify-center border border-white/10 hover:bg-black/60 hover:border-white/30 transition-all z-10"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill={isSaved ? "#ef4444" : "none"} stroke={isSaved ? "#ef4444" : "currentColor"} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
        </button>
      </div>

      {/* Content Area */}
      <div className="p-4 flex flex-col flex-1 bg-surface group-hover:bg-surface/80 transition-colors">
        <div className={`flex gap-2 text-[10px] font-mono uppercase mb-1 items-center ${isCurated ? 'text-[#c4b5fd]' : 'text-lime-400'}`}>
          {product.brandLogo ? (
            <img src={product.brandLogo} alt={product.brandName} className="h-4 object-contain mr-1" onError={(e) => e.target.style.display = 'none'} />
          ) : product.brandName ? (
            <span className="text-white/80 font-bold">{product.brandName}</span>
          ) : null}
          {product.category && <span>{product.category}</span>}
          {product.category && product.subcategory && <span className="text-white/30">•</span>}
          {product.subcategory && <span>{product.subcategory}</span>}
        </div>
        
        <h3 className="font-display font-bold text-lg leading-tight text-white mb-1 line-clamp-2">
          {product.productName}
        </h3>
        
        {product.vibeTags && product.vibeTags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-2 z-10">
            {product.vibeTags.slice(0, 3).map((vibe, idx) => (
              <span 
                key={idx} 
                onClick={(e) => handleVibeClick(e, vibe)}
                className={`cursor-pointer text-[10px] font-mono bg-bg border border-border px-1.5 py-0.5 rounded text-white/50 transition-colors ${
                  isCurated 
                    ? 'group-hover:border-[#c4b5fd]/30 hover:!bg-[#c4b5fd]/10 hover:!text-[#c4b5fd]' 
                    : 'group-hover:border-lime-400/30 hover:!bg-lime-400/10 hover:!text-lime-400'
                }`}
              >
                {vibe}
              </span>
            ))}
          </div>
        )}
        
        <div className="flex-1"></div>
        
        <div className="flex items-center justify-between mt-4 border-t border-border/50 pt-3">
          <div className="flex flex-col">
            <span className="text-xs font-mono text-white/40">Est. Price</span>
            <span className="font-display font-medium text-lg text-white">
              {(product.price || product.estimatedPrice) ? `₹${product.price || product.estimatedPrice}` : 'TBA'}
            </span>
          </div>
          
          <div className="flex items-center gap-3">
            {!isCurated && product.trendScore && (
              <ScoreRing score={product.trendScore} size={36} strokeWidth={3} />
            )}
            {buyUrl && (
              <button 
                onClick={handleBuy}
                className={`bg-white text-black font-mono text-xs font-bold px-4 py-2 rounded-lg transition-colors flex items-center justify-center min-w-[60px] ${isCurated ? 'hover:bg-[#c4b5fd]' : 'hover:bg-lime-400'}`}
              >
                {buyLabel}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
