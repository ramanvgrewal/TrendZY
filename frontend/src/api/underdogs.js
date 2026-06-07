import axios from './axios';

export const getUnderdogs = (section = null, page = 0, size = 20) => {
    const params = { page, size };
    if (section && section !== 'ALL') params.vibe = section;
    return axios
        .get('/api/curated', { params })
        .then((r) => {
            const data = r.data?.data ?? r.data;
            return Array.isArray(data) ? data : (data?.content || []);
        });
};

export const getUnderdogSections = () =>
    axios.get('/api/curated/sections').catch(() => ({ data: { data: [] } })).then((r) => r.data?.data ?? r.data ?? []);