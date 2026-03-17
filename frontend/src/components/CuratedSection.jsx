import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { fetchFeaturedCurated } from '../api/client';
import CuratedCard from './CuratedCard';

export default function CuratedSection() {
    const [products, setProducts] = useState([]);
    const scrollRef = useRef(null);

    useEffect(() => {
        (async () => {
            const data = await fetchFeaturedCurated();
            setProducts(data.slice(0, 8));
        })();
    }, []);

    const scroll = (direction) => {
        if (scrollRef.current) {
            const amount = direction === 'left' ? -320 : 320;
            scrollRef.current.scrollBy({ left: amount, behavior: 'smooth' });
        }
    };

    if (products.length === 0) return null;

    return (
        <section className="curated-section anim-fade-up">
            <div className="curated-section-inner">
                {/* Header */}
                <div className="curated-header">
                    <div>
                        <h2 className="curated-title">
                            <span className="curated-star">✦</span> ONLY ON TRENDZY
                        </h2>
                        <p className="curated-subtitle">
                            Hidden gems from indie brands — you won't find these anywhere else
                        </p>
                    </div>
                    <div className="curated-header-right">
                        <div className="hidden sm:flex gap-2 mr-3">
                            <button onClick={() => scroll('left')} className="curated-scroll-btn">&larr;</button>
                            <button onClick={() => scroll('right')} className="curated-scroll-btn">&rarr;</button>
                        </div>
                        <Link to="/only-on-trendzy" className="curated-explore-link">
                            Explore All →
                        </Link>
                    </div>
                </div>

                {/* Horizontal scroll row */}
                <div ref={scrollRef} className="curated-scroll-row scrollbar-hide">
                    {products.map((p) => (
                        <div key={p.id} className="curated-scroll-item">
                            <CuratedCard product={p} />
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
