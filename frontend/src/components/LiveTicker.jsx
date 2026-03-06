import { useState, useEffect } from 'react';
import { fetchTrends } from '../api/client';

export default function LiveTicker() {
    const [items, setItems] = useState([]);

    useEffect(() => {
        (async () => {
            const trends = await fetchTrends();
            setItems(trends.slice(0, 12).map(t => `${t.productName} — ${t.velocityLabel || 'Emerging'}`));
        })();
    }, []);

    if (items.length === 0) return null;

    const text = items.join('   ·   ');

    return (
        <div className="ticker-wrap" style={{ height: 28 }}>
            <div className="ticker-content flex items-center h-full"
                style={{ fontSize: 11, fontWeight: 600, letterSpacing: '1.5px', color: '#05070A', textTransform: 'uppercase' }}>
                <span className="px-4">{text}   ·   {text}</span>
            </div>
        </div>
    );
}
