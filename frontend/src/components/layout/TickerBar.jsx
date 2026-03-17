import { useQuery } from '@tanstack/react-query';
import { getTickerData } from '../../api/trends';

export default function TickerBar() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['ticker'],
    queryFn: getTickerData
  });

  if (isError) return null; // hide on error

  return (
    <div className="ticker-wrap h-8 flex items-center text-xs font-mono text-lime-400">
      <div className="ticker-track space-x-8 px-4">
        {isLoading ? (
          <span>Loading trends...</span>
        ) : (
          data && data.length > 0 ? (
            // Duplicate the items a few times to ensure seamless infinite scroll
            [...Array(4)].map((_, i) => (
              <div key={i} className="flex space-x-8">
                {data.map((item, idx) => (
                  <span key={`${i}-${idx}`} className="flex items-center gap-2">
                    <span className="text-white">{item.productName}</span>
                    {item.velocityLabel && (
                      <span className="text-lime-400 uppercase">▲{item.velocityLabel}</span>
                    )}
                    <span className="text-white/30 ml-4">·</span>
                  </span>
                ))}
              </div>
            ))
          ) : null
        )}
      </div>
    </div>
  );
}
