import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSessionId } from '../utils/session';
import { recordBuyClick, recordView } from '../api/client';

function scoreClass(score) {
  if (score >= 7) return 'text-scorehigh';
  if (score >= 5) return 'text-scoremedium';
  return 'text-foreground';
}

export default function TrendCard({ trend }) {
  const navigate = useNavigate();
  const score = trend.trendScore ?? 0;
  const mentions = trend.mentionCountThisWeek ?? 0;
  const [imgError, setImgError] = useState(false);

  const hasImage = trend.imageUrl && trend.imageUrl.trim() !== '' && !imgError;

  const openDetail = () => {
    recordView(getSessionId(), trend.id);
    // FIX: was /trend/${id} which doesn't exist — correct route is /product/:id
    navigate(`/product/${trend.id}?tier=trending`);
  };

  const onBuy = (e, link) => {
    e.stopPropagation();
    if (!link?.affiliateUrl || link.affiliateUrl === '#') return;
    recordBuyClick(getSessionId(), trend.id);
    window.open(link.affiliateUrl, '_blank', 'noopener,noreferrer');
  };

  const firstLink = trend.productLinks?.[0];
  const secondLink = trend.productLinks?.[1] || trend.productLinks?.[0];

  return (
      <article
          onClick={openDetail}
          className="group card-hover rounded-xl border border-border bg-card overflow-hidden cursor-pointer"
      >
        {/* Product Image */}
        {hasImage && (
            <div className="trend-card-image">
              <img
                  src={trend.imageUrl}
                  alt={trend.productName}
                  loading="lazy"
                  onError={() => setImgError(true)}
              />
              {trend.imageSource && (
                  <span className="trend-card-img-badge">
              {trend.imageSource === 'amazon' ? 'Amazon'
                  : trend.imageSource === 'myntra' ? 'Myntra'
                      : trend.imageSource}
            </span>
              )}
            </div>
        )}

        <div className="p-5">
          <div className="-mx-5 -mt-5 mb-4 h-0.5 bg-gradient-to-r from-primary via-secondary to-primary" />

          {/* Source badges + score */}
          <div className="mb-4 flex flex-wrap items-center gap-2">
            {trend.sourcePlatforms?.includes('youtube') && (
                <span className="rounded-md bg-[#ff0000]/10 px-2.5 py-1 font-body text-[11px] font-medium uppercase tracking-wider text-[#ff0000]">
              YouTube
            </span>
            )}
            {(!trend.sourcePlatforms || trend.sourcePlatforms.length === 0 || trend.sourcePlatforms?.includes('reddit')) && (
                <span className="rounded-md bg-[#ff4500]/10 px-2.5 py-1 font-body text-[11px] font-medium uppercase tracking-wider text-[#ff4500]">
              Reddit
            </span>
            )}
            <span className={`ml-auto font-display text-3xl font-bold leading-none ${scoreClass(score)}`}>
            {score.toFixed(1)}
          </span>
          </div>

          {/* Name */}
          <h3 className="mb-2 font-display text-lg font-bold text-foreground transition-colors duration-300 group-hover:text-primary">
            {trend.productName}
          </h3>

          {/* AI explanation */}
          <p className="mb-3 line-clamp-2 font-body text-sm text-muted">
            {trend.aiExplanation || 'Signal detected — analysis in progress.'}
          </p>

          {/* Tags */}
          <div className="mb-3 flex flex-wrap gap-2">
            {trend.category && (
                <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
              {trend.category}
            </span>
            )}
            {trend.brandMention && (
                <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
              {trend.brandMention}
            </span>
            )}
            <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
            🇮🇳 India
          </span>
          </div>

          {/* Vibe tags */}
          {trend.vibeTags && trend.vibeTags.length > 0 && (
              <div className="mb-3 flex flex-wrap gap-2">
                {trend.vibeTags.slice(0, 3).map((vibe) => (
                    <span
                        key={vibe}
                        className="rounded-md px-2 py-1 font-body text-[10px] uppercase tracking-wider"
                        style={{
                          background: 'var(--color-lime-dim, rgba(204,255,0,0.1))',
                          color: 'var(--color-lime, #ccff00)',
                          border: '1px solid rgba(204,255,0,0.2)',
                        }}
                    >
                #{vibe}
              </span>
                ))}
              </div>
          )}

          {/* Mentions */}
          <div className="mb-4 flex items-center gap-2 font-body text-xs text-muted">
            <span>{mentions.toLocaleString()} mentions</span>
            <span className="rounded-md bg-primary/20 px-2 py-0.5 text-primary">New</span>
          </div>

          {/* Buy buttons */}
          <div className="grid grid-cols-2 gap-2">
            <button
                onClick={(e) => onBuy(e, firstLink)}
                disabled={!firstLink?.affiliateUrl}
                className="rounded-lg border border-primary bg-primary px-3 py-2 font-body text-xs font-medium uppercase tracking-wider text-background transition-all duration-200 hover:bg-secondary hover:border-secondary disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Amazon
            </button>
            <button
                onClick={(e) => onBuy(e, secondLink)}
                disabled={!secondLink?.affiliateUrl}
                className="rounded-lg border border-border bg-transparent px-3 py-2 font-body text-xs font-medium uppercase tracking-wider text-foreground transition-all duration-200 hover:border-secondary/50 hover:text-secondary disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Myntra
            </button>
          </div>
        </div>
      </article>
  );
}