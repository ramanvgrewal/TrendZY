import axios from './axios';

export const getTrends = (tier, vibe, page = 0, size = 20) =>
  axios.get('/api/trends', { params: { tier, vibe, page, size } })
  .then(r => r.data.data);

export const getHeroTrend = () =>
  axios.get('/api/trends/top').then(r => r.data.data);

export const getTrendById = (id) =>
  axios.get(`/api/trends/${id}`).then(r => r.data.data);

export const getRelatedTrends = (id, size = 6) =>
  axios.get(`/api/trends/${id}/related`, { params: { size } })
  .then(r => r.data.data);

export const getTrendStats = () =>
  axios.get('/api/trends/stats').then(r => r.data.data);

export const getTickerData = () =>
  axios.get('/api/trends/ticker').then(r => r.data.data);
