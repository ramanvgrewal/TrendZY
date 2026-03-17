import axios from './axios';

export const getCurated = (vibe, brand, page = 0, size = 20) =>
  axios.get('/api/curated/all', { params: { vibe, brand, page, size } })
  .then(r => r.data.data);

export const getCuratedById = (id) =>
  axios.get(`/api/curated/${id}`).then(r => r.data.data);

export const getCuratedBrands = () =>
  axios.get('/api/curated/brands').then(r => r.data.data);

export const getFeaturedCurated = () =>
  axios.get('/api/curated/featured').then(r => r.data.data);
