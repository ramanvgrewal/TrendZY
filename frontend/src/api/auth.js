import axios from './axios';

export const login = (email, password) =>
  axios.post('/api/auth/login', { email, password })
  .then(r => r.data.data);

export const register = (name, email, password) =>
  axios.post('/api/auth/register', { name, email, password })
  .then(r => r.data.data);
