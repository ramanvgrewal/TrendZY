import axios from './axios';

export const searchProducts = (q, type, vibe, page = 0) =>
  axios.get('/api/search', { params: { q, type, vibe, page, size: 20 } })
  .then(r => r.data.data);

export const getAutocomplete = (q) =>
  axios.get('/api/search/autocomplete', { params: { q } })
  .then(r => r.data.data);

export const getTrendingQueries = () =>
  axios.get('/api/search/trending-queries').then(r => r.data.data);
