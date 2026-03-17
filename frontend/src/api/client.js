import axios from 'axios';
import { mockTrends, mockCategories } from './mockData';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    timeout: 8000,
    headers: { 'Content-Type': 'application/json' }
});

// Track backend status for offline banner
let backendOffline = false;
export const isBackendOffline = () => backendOffline;

/* ── Stats API ── */

export async function fetchStats() {
    try {
        const { data } = await api.get('/api/stats');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return {
            totalSignals: '—',
            activeTrends: mockTrends.length,
            indiaRelevantCount: mockTrends.filter(t => t.indiaRelevant).length,
            subredditsMonitored: 24
        };
    }
}

/* ── Trend APIs ── */

export async function fetchTrends(params = {}) {
    try {
        const { data } = await api.get('/api/trends', { params });
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        let filtered = [...mockTrends];
        if (params.category) filtered = filtered.filter(t => t.category?.toLowerCase() === params.category.toLowerCase());
        if (params.gender) filtered = filtered.filter(t => t.gender === params.gender);
        if (params.pricePoint) filtered = filtered.filter(t => t.pricePoint === params.pricePoint);
        if (params.indiaRelevant) filtered = filtered.filter(t => t.indiaRelevant);
        if (params.audience) filtered = filtered.filter(t => t.audienceTag === params.audience);
        return filtered;
    }
}

export async function fetchRisingTrends() {
    try {
        const { data } = await api.get('/api/trends/rising');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return mockTrends.filter(t => t.velocityLabel === 'Rising Fast');
    }
}

export async function fetchTrendById(id) {
    try {
        const { data } = await api.get(`/api/trends/${id}`);
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return mockTrends.find(t => t.id === Number(id)) || null;
    }
}

export async function fetchCategories() {
    try {
        const { data } = await api.get('/api/trends/categories');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return mockCategories;
    }
}

export async function fetchVibes() {
    try {
        const { data } = await api.get('/api/trends/vibes');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return ["aesthetic", "Y2K", "minimalist", "streetwear"];
    }
}

export async function fetchSections() {
    try {
        const { data } = await api.get('/api/trends/sections');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        // mock sections
        return [
            {
                subcategory: "Sneakers",
                emoji: "👟",
                topVibes: ["streetwear"],
                trends: mockTrends.slice(0, 4)
            }
        ];
    }
}

/* ── Session / Personalisation APIs ── */

export async function recordView(sessionId, trendId) {
    try {
        await api.post('/api/session/view', { sessionId, trendId });
    } catch {
        // silent — non-critical
    }
}

export async function recordBuyClick(sessionId, trendId) {
    try {
        await api.post('/api/session/buy-click', { sessionId, trendId });
    } catch {
        // silent — non-critical
    }
}

/* ── Curated Products (ONLY ON TRENDZY) ── */

export async function fetchCuratedProducts(category) {
    try {
        const params = category ? { category } : {};
        const { data } = await api.get('/api/curated', { params });
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return [];
    }
}

export async function fetchFeaturedCurated() {
    try {
        const { data } = await api.get('/api/curated/featured');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return [];
    }
}

export async function fetchCuratedCategories() {
    try {
        const { data } = await api.get('/api/curated/categories');
        backendOffline = false;
        return data;
    } catch {
        backendOffline = true;
        return [];
    }
}

export async function saveCuratedProduct(product) {
    const { data } = await api.post('/api/curated', product);
    return data;
}

export async function updateCuratedProduct(id, product) {
    const { data } = await api.put(`/api/curated/${id}`, product);
    return data;
}

export async function deleteCuratedProduct(id) {
    await api.delete(`/api/curated/${id}`);
}

export async function bulkImportCurated(products) {
    const { data } = await api.post('/api/curated/bulk', products);
    return data;
}

export default api;
