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

  const openDetail = () => {
    recordView(getSessionId(), trend.id);
    navigate(`/trend/${trend.id}`);
  };

  const onBuy = (event, link) => {
    event.stopPropagation();
    recordBuyClick(getSessionId(), trend.id);
    window.open(link.affiliateUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <article
      onClick={openDetail}
      className="group card-hover rounded-xl border border-border bg-card p-5"
    >
      <div className="-mx-5 -mt-5 mb-4 h-0.5 rounded-t-xl bg-gradient-to-r from-primary via-secondary to-primary" />

      <div className="mb-4 flex items-center justify-between gap-3">
        <span className="rounded-md bg-primary/10 px-2.5 py-1 font-body text-[11px] font-medium uppercase tracking-wider text-primary">
          ?? Trending Now
        </span>
        <span className={`font-display text-3xl font-bold leading-none ${scoreClass(score)}`}>
          {score.toFixed(1)}
        </span>
      </div>

      <h3 className="mb-2 font-display text-lg font-bold text-foreground transition-colors duration-300 group-hover:text-primary">
        {trend.productName}
      </h3>

      <p className="mb-3 line-clamp-2 font-body text-sm text-muted">
        {trend.aiExplanation || 'Signal detected. AI explanation in progress.'}
      </p>

      <div className="mb-3 flex flex-wrap gap-2">
        <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
          {trend.category || 'other'}
        </span>
        {trend.brandMention && (
          <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
            {trend.brandMention}
          </span>
        )}
        <span className="rounded-md bg-surface-elevated px-2 py-1 font-body text-[11px] uppercase tracking-wider text-muted">
          ???? India
        </span>
      </div>

      <div className="mb-4 flex items-center gap-2 font-body text-xs text-muted">
        <span>{mentions.toLocaleString()} mentions</span>
        <span className="rounded-md bg-primary/20 px-2 py-0.5 text-primary">New</span>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <button
          onClick={(event) => onBuy(event, trend.productLinks?.[0] || { affiliateUrl: '#' })}
          className="rounded-lg border border-primary bg-primary px-3 py-2 font-body text-xs font-medium uppercase tracking-wider text-background transition-all duration-200 hover:bg-secondary hover:border-secondary"
        >
          Amazon
        </button>
        <button
          onClick={(event) => onBuy(event, trend.productLinks?.[1] || trend.productLinks?.[0] || { affiliateUrl: '#' })}
          className="rounded-lg border border-border bg-transparent px-3 py-2 font-body text-xs font-medium uppercase tracking-wider text-foreground transition-all duration-200 hover:border-secondary/50 hover:text-secondary"
        >
          Myntra
        </button>
      </div>
    </article>
  );
}
