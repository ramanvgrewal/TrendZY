import axios from './axios';

export const getMe = () =>
  axios.get('/api/users/me').then(r => r.data.data);

export const getSaved = () =>
  axios.get('/api/users/saved').then(r => r.data.data);

export const saveProduct = (productId) =>
  axios.post(`/api/users/save/${productId}`).then(r => r.data);

export const unsaveProduct = (productId) =>
  axios.delete(`/api/users/save/${productId}`).then(r => r.data);
