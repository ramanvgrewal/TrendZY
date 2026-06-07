import { useQuery } from '@tanstack/react-query';
import { getTickerData } from '../../api/trends';

export default function TickerBar() {
    const { data, isLoading, isError } = useQuery({
        queryKey: ['ticker'],
        queryFn: getTickerData,
        staleTime: 5 * 60 * 1000,
    });

    if (isLoading || isError || !data || data.length === 0) return null;

    const tickerText = data
        .map((item) => `${item.productName}${item.velocityLabel ? '  ▲ ' + item.velocityLabel : ''}`)
        .join('   ·   ');

    // Duplicate for seamless loop
    const content = `${tickerText}   ·   ${tickerText}`;

    return (
        <div className="ticker-wrap h-8 overflow-hidden">
            <div className="ticker-track flex items-center h-full">
        <span
            className="font-mono text-[11px] font-semibold uppercase tracking-wider text-lime-400 whitespace-nowrap"
            style={{ paddingLeft: '100%' }}
        >
          {content}
        </span>
            </div>
        </div>
    );
}