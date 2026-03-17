import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchTrends } from '../api/client';
import TrendCard from '../components/TrendCard';

export default function CategoryPage() {
    const { subcategory } = useParams();
    const navigate = useNavigate();
    const [trends, setTrends] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            const data = await fetchTrends({ subcategory, indiaRelevant: true });
            setTrends(data);
            setLoading(false);
        };
        load();
    }, [subcategory]);

    return (
        <div className="min-h-screen bg-background text-foreground">
            <main className="mx-auto max-w-7xl space-y-10 px-4 py-8 sm:px-6 lg:px-8">
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-muted hover:text-foreground transition-colors font-body text-sm mb-4"
                >
                    &larr; Back to Home
                </button>

                <section>
                    <h1 className="font-display text-[clamp(2rem,5vw,4rem)] font-bold capitalize text-primary mb-2">
                        {subcategory}
                    </h1>
                    <p className="font-body text-muted">
                        Explore all trending {subcategory} items for the Indian market.
                    </p>
                </section>

                <section className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
                    {loading ? (
                        <p className="text-muted col-span-full">Loading trends...</p>
                    ) : trends.length > 0 ? (
                        trends.map((trend) => (
                            <TrendCard key={trend.id} trend={trend} />
                        ))
                    ) : (
                        <p className="text-muted col-span-full">No trends found in this subcategory.</p>
                    )}
                </section>
            </main>
        </div>
    );
}
